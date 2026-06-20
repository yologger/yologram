# yologram-api-v2 프로젝트 지침

## 기술 스택

- FastAPI, Python 3.12+
- uv 패키지 매니저
- Pydantic Settings
- SQLAlchemy + PyMySQL (ORM + DB)
- bcrypt (비밀번호 해싱)
- PyJWT (JWT 토큰)
- boto3 (AWS SES 이메일 발송)
- pytest + httpx (테스트)

## 설정 관리

- .env 파일로 로컬 설정
- ECS secrets (Parameter Store)에서 환경변수로 주입
- DB 환경변수: DB_URL, DB_USERNAME, DB_PASSWORD
- JWT 환경변수: JWT_SECRET (Parameter Store에서 주입)
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS는 OpenTelemetry SDK가 자동으로 읽음
- SES 발신 주소: ses_from_address (기본 no-reply@yologram.link)
- AWS 자격증명: prod는 ECS Task Role, 로컬은 AWS_PROFILE 환경변수 (scripts/run-prod.sh에서 export)
- pydantic-settings: 필드명을 대문자 환경변수로 자동 매핑

## DB

- SQLAlchemy + PyMySQL
- R/W splitting 없음 (단일 writer)
- app/config/database.py: engine, SessionLocal, get_db

## API

- 응답 래퍼: ApiEnvelop ({ "data": T })
- 예외: AppException → { "errorMessage", "errorCode" }
- 입력값 검증 실패(RequestValidationError): 400 VALIDATION_ERROR, 메시지는 첫 에러를 사람이 읽을 단일 문자열로(Pydantic "Value error, " 접두 제거). status·errorCode를 api-v1과 정합(400)
- 라우팅 예외도 동일 형식: 404 → NOT_FOUND, 405 → METHOD_NOT_ALLOWED (StarletteHTTPException 핸들러)
- CORS: 전체 허용 (*)
- Swagger: /api/v2/docs
- 신규 API 추가 시 Swagger 문서화 필수 (요청/응답 스키마, 에러 코드, 인증 여부)

## 인증

- JWT: PyJWT (HMAC256), api-v1과 동일한 secret/issuer/audience
- 설정: jwt_secret(환경변수), jwt_expire(86400), jwt_issuer(yologram.link), jwt_audience(yologram.client)
- 인증 헤더: Authorization: Bearer {token}
- get_authenticated_user 의존성으로 인증 정보 주입 (FastAPI Depends)
- access token은 stateless JWT (서버에 저장하지 않음). 로그아웃은 클라이언트가 토큰을 폐기하는 방식이며, 현재 서버측 강제 무효화는 불가
- validate-token: JWT 서명/만료 검증 + 사용자 존재 확인
- (추후) refresh token 도입 시 서버측 토큰 무효화도 함께 구현 (로그아웃 시 refresh token 폐기)

## 이메일 인증

- EmailSender 프로토콜로 발송 추상화
- StubEmailSender: 로그 출력 (app_profile != prod, 개발/테스트용)
- SesEmailSender: AWS SES 발송 (app_profile == prod, boto3)
- get_email_sender 의존성으로 프로파일에 따라 주입
- 발신 주소: no-reply@yologram.link (ses_from_address 설정)
- 리전: ap-northeast-2
- 자격증명: ECS Task Role (prod), AWS_PROFILE 환경변수 (로컬, scripts/run-prod.sh)
- EmailVerificationCode 모델: email, code(6자리), verified, expired_at(5분), created_at / 테이블 email_verification_codes
- 엔드포인트: POST /api/v2/ums/auth/email-verification/send, /verify
- 회원가입 시 이메일 인증 필수 (UserService.join에서 verified 확인, 가입 후 코드 삭제)

## 비밀번호 찾기

- 방식: 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (이메일 인증과 동일 패턴/SES 재사용, api-v1과 동일)
- 저장: 별도 테이블 password_reset_codes (PasswordResetCode 모델: email, code, verified, expired_at 5분, created_at) — api-v1과 공유
- PasswordResetService: send_code(미가입 시 UserNotFoundException 404, 기존 코드 삭제 후 발송), verify_code(verified=true), confirm(email·code·new_password 재검증 후 변경·코드 삭제)
- 엔드포인트: POST /api/v2/ums/auth/password-reset/send·verify·confirm (confirm 요청 필드 newPassword)
- 예외: PasswordResetExpiredException/PasswordResetInvalidException (400)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한

## 회원탈퇴

- 현재(개발 단계): DELETE /api/v2/ums/user/me → 레코드 하드 삭제 (UserService.withdraw). email 즉시 해제되어 재가입 가능 (api-v1과 동일)
- 추후: soft delete(status=DELETED + deleted_date) 전환, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403), 유예 후 PII 익명화/하드삭제 배치, 연관 데이터 비동기 정리, 조회 시 DELETED 필터링, email 재가입 정책

## 커뮤니티 카테고리 (CMS)

- 도메인: app/domain/cms (Section enum, Category 모델/조회) — api-v1 미러링
- Section enum: TECH / INVEST / POLITICS (게시판=섹션, str Enum). 코드 결합이라 ENUM 유지
- categories 테이블: id, section, name, sort_order, is_active, created_at (api-v1과 DB 공유, UNIQUE(section, name))
- CategoryService.get_categories(section_path): Section.from_path로 검증(대소문자 무시), is_active=true, sort_order 정렬
- 응답 CategoryResponse: { id, name, sortOrder } (sort_order serialization_alias)
- 엔드포인트: GET /api/v2/cms/{section}/categories
- 예외: InvalidSectionException (400, INVALID_SECTION) — core/exception.py
- 카테고리는 어드민이 관리하는 콘텐츠(추후 CRUD), 프론트는 이 API로 섹션별 필터를 동적 렌더

## 커뮤니티 게시글 (PMS)

- 도메인: app/domain/pms (Post, PostCategory). 작성은 단일 엔드포인트 POST /api/v2/pms/{section}/posts (인증 필요) — api-v1 미러링
- community_posts / post_categories(N:M) (api-v1과 DB 공유). 경계 넘는 참조(user_id, category_id)는 FK 없이 인덱스
- PostService.create: 작성자=인증 유저(uid), categoryIds가 해당 section 활성 카테고리인지 검증 (1~3개 필수). 프론트에서도 카테고리 1개 이상 선택해야 작성 가능(미선택 시 버튼 비활성)
- CategoryQueryClient(Protocol) + LocalCategoryQueryClient로 cms 카테고리 검증 추상화 → 모놀리식은 cms 리포지토리 직접, MSA 분리 시 HTTP 호출 구현으로 교체
- 요청 { title?, content, categoryIds[] }, 응답 { id } (201)
- 예외: InvalidCategoryException (400, INVALID_CATEGORY), 잘못된 section은 Section.from_path의 InvalidSectionException (400)
- 검증 메시지는 api-v1과 동일 문구 ("내용을 입력해주세요.", "카테고리는 1~3개 선택해주세요.")
- 상세 조회: GET /api/v2/pms/{section}/posts/{id} (공개). PostDetailResponse에 author{uid,nickname} 포함(UserQueryClient로 ums 조회, MSA 대비), categoryIds는 프론트가 매핑. 없거나 다른 section의 id면 404 POST_NOT_FOUND
- 목록 조회: GET /api/v2/pms/{section}/posts (공개). 최신순(id desc) + cursor(keyset) 페이지네이션, categoryId 필터(옵션), size 기본 20·최대 50. 응답 ApiEnvelopCursorPage{data, nextCursor}, PostSummaryResponse(content 전체 포함) — api-v1과 동일
- 커서/종료 판정은 api-v1과 동일(legacy 방식): id-only 커서(Base64) + 마지막 글 id를 nextCursor로(빈 결과면 null). 잘못된 커서 → 400 INVALID_CURSOR
- N+1 회피: find_nicknames(ums 경계 추상화)·find_by_post_ids 배치 조회. 카테고리는 1:N이라 join 대신 IN, EXISTS로 categoryId 필터
- DB·인덱스(community_posts (section, id))는 api-v1과 공유

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- pytest + TestClient
- mock: unittest.mock (MagicMock, patch)
- uv run pytest tests/ -v

## Observability

- Grafana Cloud OTLP direct push
- Logs: OpenTelemetry LoggerProvider + OTLPLogExporter (app/config/logging.py)
- Metrics: OpenTelemetry MeterProvider + OTLPMetricExporter + SystemMetricsInstrumentor (app/config/metrics.py)
- Traces: OpenTelemetry TracerProvider + OTLPSpanExporter + FastAPIInstrumentor (app/config/tracing.py)
- Resource 속성: service.name, deployment.environment.name, service.instance.id, service.namespace

## 포트

- 로컬/ECS 모두 5000

## 배포

- Docker (python:3.12-slim multi-stage)
- ECS Fargate
- GitHub Actions: Docker build → ECR push → ECS 재배포
