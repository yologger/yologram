# yologram-api-v2 구현 기능 및 설계 근거

구현 완료된 기능과 그 설계 근거를 기록한다. (앞으로 할 일은 tasks.md)

api-v1(Spring Boot)을 FastAPI로 미러링한 프로젝트로, 기능·설계가 거의 동일하다. 두 프로젝트 비교분석을 통한 학습이 목적이며 객체지향 설계·Layered Architecture·의존성 주입을 적용한다.

## 구현된 기능

### 기반/공통
- 프로젝트 구성: uv init, 디렉토리 구조(app/config, app/domain, app/core), .env/.env.prod/.env.staging
- 설정: pydantic BaseSettings(Settings), APP_PROFILE로 .env.prod/.env.staging 분기
- DB: SQLAlchemy + PyMySQL, engine + SessionLocal, get_db 의존성 (app/config/database.py)
- 응답 래퍼 ApiEnvelop ({ "data": T })
- 예외 처리 (AppException → { errorMessage, errorCode })
- CORS 전체 허용 (*)
- 컨테이너화: Dockerfile(python:3.12-slim multi-stage), GitHub Actions(ECR push → ECS 재배포)

### Test 도메인 — /api/v2/test
- GET /api/v2/test (기본 응답), /echo(요청 정보), /profile(활성 프로파일), /property?key=...(설정값 조회)

### UMS (회원/인증) — /api/v2/ums
- 회원가입 POST /user/join (이메일 인증 필수)
- 로그인 POST /auth/login, 로그아웃 POST /auth/logout (204), 토큰 검증 POST /auth/validate-token
- 회원정보 조회 GET /user/me, 수정 PATCH /user/me(이름·닉네임), 비밀번호 변경 PATCH /user/me/password
- 이메일 인증 POST /auth/email-verification/send·verify (AWS SES)
- 비밀번호 찾기 POST /auth/password-reset/send·verify·confirm
- 회원탈퇴 DELETE /user/me (현재 하드 삭제)

### CMS (카테고리) — /api/v2/cms
- 카테고리 조회 GET /{section}/categories (is_active=true, sort_order 정렬)

### PMS (게시글) — /api/v2/pms
- 작성 POST /{section}/posts (인증, categoryIds 1~3 검증)
- 상세 GET /{section}/posts/{id} (공개)
- 목록 GET /{section}/posts (공개, id desc cursor 페이지네이션, categoryId 필터)

### 인프라/공통
- Observability: Grafana Cloud OTLP direct push (logs/metrics/traces)
  - Logs: LoggerProvider + OTLPLogExporter (app/config/logging.py)
  - Metrics: MeterProvider + OTLPMetricExporter + SystemMetricsInstrumentor (app/config/metrics.py)
  - Traces: TracerProvider + OTLPSpanExporter + FastAPIInstrumentor (app/config/tracing.py)
  - Resource 속성: service.name, deployment.environment.name, service.instance.id, service.namespace
- Swagger /api/v2/docs

## Spring Boot(v1) ↔ FastAPI(v2) 대응 구조

| Spring Boot (v1) | FastAPI (v2) | 비고 |
|---|---|---|
| @RestController | APIRouter (클래스 기반) | FastAPI는 함수 기반이 기본이나 class-based로 구성 |
| @Service | Service 클래스 | 비즈니스 로직 계층 |
| @Repository | Repository 클래스 | 데이터 접근 계층 |
| @Autowired / 생성자 주입 | Depends() | FastAPI 의존성 주입 |
| application-{profile}.yaml | .env.{profile} | pydantic-settings |
| Environment.getProperty() | Settings 클래스 | pydantic BaseSettings |
| Spring Profiles | APP_PROFILE 환경변수 | prod, staging, default 분기 |
| aws-parameterstore import | ECS Task Definition secrets | 인프라 레벨 환경변수 주입 |

## 설계 근거

### 설정 관리
- 로컬/일반 설정: .env 파일. pydantic-settings BaseSettings가 .env 기본 지원, APP_PROFILE로 분기. 12-Factor App·Python 생태계 보편 방식
- Secret: AWS Parameter Store 저장 → ECS Task Definition secrets 블록으로 컨테이너 환경변수 주입. 코드는 os.environ 접근(프레임워크 의존 없음)
- Spring Boot는 프레임워크가 Parameter Store를 직접 통합하지만, FastAPI(및 대부분 프레임워크)는 인프라(ECS/K8s)에 secret 주입을 맡기는 게 표준

### 결정 사항
- 패키지 매니저: uv
- 설정 관리: pydantic-settings + .env (YAML 아님)
- Secret: ECS Task Definition secrets (앱 코드에서 직접 안 가져옴)
- Python 3.12+

### 도메인 구조 (모듈러 모놀리식 → MSA 대비)
- 단일 서버에서 도메인별 패키지로 분리. 도메인 경계 = 미래 MSA 경계 = API Gateway path prefix
  - UMS→yologram-user-api, PMS→yologram-post-api, CMS→yologram-cms-api, Comment→comment-api, Count→count-api, News→news-api
- 분리 전략: 도메인 간 직접 JOIN·repository 교차 호출 금지
  - 경계 호출은 인터페이스(QueryClient/Protocol)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: PostCategoryQueryClient, UserQueryClient)
  - self HTTP 호출 지양. 모놀리식 단계는 직접 호출(Local*QueryClient) 구현
  - 경계 넘는 FK 지양(같은 도메인 내부만 FK). 경계 넘는 참조는 인덱스 + app-level 검증
  - 경계 넘는 동기 트랜잭션 의존 최소화 (count 갱신은 추후 이벤트/최종일관성)
- 도메인 분리(pms/cms/comment/count/news)·하이브리드 스키마 결정은 api-v1 docs/brainstorm.md에 상세. api-v2는 동일 설계 미러링

### API 경로 규칙
- /api/v2/ums/ (유저), /api/v2/pms/{section}/ (게시글), /api/v2/cms/{section}/categories (카테고리)
- /api/v2/comments/ (댓글, 추후), /api/v2/count/ (카운트, 예약), /api/v2/news/ (뉴스, 추후)
- 어드민: 도메인 경로 뒤 admin 세그먼트 → /api/v2/{domain}/admin/... (게이트웨이 라우팅과 일관)

### 유저 타입
- DEFAULT(일반), POLITICIAN(정치인), ECONOMIST(경제인), ADMIN(관리자) — UserType/UserStatus enum (VARCHAR 저장)

### 인증 방식
- JWT(HMAC256, PyJWT), Authorization: Bearer 헤더. api-v1과 동일한 secret/issuer/audience
- 설정: jwt_secret(환경변수 JWT_SECRET, Parameter Store), jwt_expire(86400), jwt_issuer(yologram.link), jwt_audience(yologram.client)
- jwt_util.py: create_token(uid), validate_and_get_uid(token)
- get_authenticated_user 의존성(Bearer 헤더 → JWT 검증 → AuthData)
- access token은 stateless (서버 미저장). 다중 로그인 지원 위해 DB 토큰 비교 제거 → 로그아웃은 클라이언트 토큰 폐기, 서버측 강제 무효화는 refresh token 도입 시 함께 구현
- validate-token은 JWT 서명/만료 검증 + 사용자 존재 확인
- 예외: AuthWrongPasswordException(401), AuthTokenExpiredException(401), AuthTokenInvalidException(401)

### 이메일 인증
- EmailSender 프로토콜로 발송 추상화 (개발: StubEmailSender 로그, 프로덕션: SesEmailSender boto3). get_email_sender 의존성이 프로파일에 따라 주입
- 발신 주소 no-reply@yologram.link (ses_from_address), 리전 ap-northeast-2. 자격증명: prod ECS Task Role, 로컬 AWS_PROFILE(scripts/run-prod.sh)
- UserEmailVerification 테이블(email, code 6자리, verified, expired_at 5분, created_at). 코드 발송→검증(verified=true)→가입 시 확인→가입 후 삭제. 재발송 시 기존 삭제 후 생성

### 비밀번호 찾기
- 이메일 6자리 코드 발송→검증→재설정 (이메일 인증과 동일 패턴/SES 재사용, api-v1과 동일)
- 별도 테이블 user_password_reset_code (회원가입 인증 로직과 분리해 회귀 위험 최소화, api-v1과 공유). UserPasswordResetCode 모델: email, code, verified, expired_at 5분, created_at
- send(미가입 404 USER_NOT_FOUND, 기존 코드 삭제 후 발송) → verify(verified=true, 프론트 게이팅) → confirm(email·code·newPassword 재검증 후 변경·코드 삭제)
- 예외: UserPasswordResetExpiredException/UserPasswordResetInvalidException (400)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한 (현재 평문·무제한)

### 회원탈퇴 데이터 정리 전략 (soft delete 전환 시 결정)
- 현재(개발 단계): 레코드 즉시 하드 삭제(UserService.withdraw) → email 해제로 재가입 가능 (api-v1과 동일)
- 탈퇴 요청과 연관 데이터(게시글/댓글/좋아요) 삭제를 분리해 요청 부하 완화
- 1차(동기, 즉시 응답): status=DELETED + deleted_date 기록, access_token 무효화 후 204. 조회 시 DELETED 필터링 → 즉시 탈퇴 효과
- 2차(비동기 연관 삭제) 옵션:
  - SQS 이벤트 + 워커 (권장, 확장성): "user-deleted" 발행 → 워커가 도메인별 배치 삭제, 실패 시 DLQ 재시도
  - 배치 잡 (간단): DELETED 유저를 주기 스캔해 청크 삭제, 별도 인프라 최소
  - 앱 내 BackgroundTasks (소규모/임시): 요청과 분리하나 인스턴스 종속 → 배포/장애 시 유실 위험, 대량 부적합
- 대량 삭제는 청크(LIMIT N) 단위 반복으로 락/replica 지연 완화. 보관 의무 데이터는 익명화·유예기간(복구) 검토(soft delete면 자연 지원)
- 권장: soft delete 즉시 응답 + SQS 워커 도메인별 청크 삭제 (규모 작으면 배치 잡으로 시작)

### 커뮤니티 게시글 데이터 모델: 하이브리드 (단일 + section)
- post (단일 테이블 + section 컬럼): 공통 필드(id, section, user_id, title, content, like_count, comment_count, created_at)
  - 섹션별 전용 필수 필드(투자 종목코드/수익률 등)는 1:1 확장 테이블로 분리(예: invest_post_detail), 해당 섹션 구현 시 추가
- 단일 테이블 채택 이유: 댓글/좋아요/저장/신고 등 상호작용이 섹션 무관 동일 → 자식 테이블·로직을 section마다 복제 안 함. 섹션 페이지·내 글·어드민은 모두 WHERE section=?로 처리. "전용 필드만" 갈리니 그 부분만 확장 테이블(하이브리드)
- 성능: 복합 인덱스 (section, id) = idx_post_section_id로 범위 스캔. 페이지네이션은 cursor(keyset), OFFSET 지양. 더 커지면 파티셔닝 검토
- post / post_category_mapping 테이블은 api-v1과 DB 공유. 경계 넘는 참조(user_id, category_id)는 FK 없이 인덱스. Post/PostCategoryMapping 모델, PostRepository/PostCategoryMappingRepository

### 커서 페이지네이션: id-only (legacy 방식과 일치)
- id가 auto_increment라 작성순=시간순 → id desc만으로 최신순. (section, id) 인덱스로 필터+정렬+커서를 한 번에 커버
- created_at 복합 커서는 정렬 기준이 id와 어긋날 때(인기순 등)나 정렬 키가 비유니크일 때만 필요 — 단순 최신순 피드엔 과함
- 종료 판정: +1/hasNext/count 없이 "마지막 글 id를 항상 nextCursor로, 빈 결과면 null". 클라이언트(TanStack useInfiniteQuery)는 nextCursor 유무로만 판단
- 구현: ApiEnvelopCursorPage[T]{data, nextCursor} / PostSummaryResponse(content 전체 포함) / PostCursor(id-only Base64). 잘못된 커서 → 400 INVALID_CURSOR
- N+1 회피: find_nicknames(UserRepository.find_by_ids, ums 경계 추상화)·find_by_post_ids 배치 조회. 카테고리는 1:N이라 join 대신 IN, categoryId 필터는 EXISTS

### 게시글 상세 조회
- GET /api/v2/pms/{section}/posts/{id} (공개). PostDetailResponse에 author{uid, nickname} 포함(UserQueryClient/LocalUserQueryClient로 ums 조회, MSA 대비), categoryIds는 프론트가 매핑
- PostRepository.find_by_id, PostCategoryRepository.find_by_post_id. 없거나 다른 section의 id면 404 POST_NOT_FOUND (PostNotFoundException)

### 게시글 작성 검증
- PostService.create: 작성자=인증 유저(uid), categoryIds가 해당 section 활성 카테고리인지 검증(1~3개 필수). 프론트도 카테고리 1개 이상 선택해야 작성 가능(미선택 시 버튼 비활성)
- 요청 { title?, content, categoryIds[] }, 응답 { id } (201)
- 예외: InvalidPostCategoryException(400 INVALID_POST_CATEGORY), 잘못된 section은 InvalidSectionException(400)
- 검증 메시지는 api-v1과 동일 문구 ("내용을 입력해주세요.", "카테고리는 1~3개 선택해주세요.")

### 검증 응답 통일 (api-v1 정합)
- RequestValidationError 핸들러: status 422 → 400, errorCode VALIDATION_ERROR, 메시지는 첫 에러를 사람이 읽을 단일 문자열로(Pydantic "Value error, " 접두 제거)
- 라우팅 예외도 동일 형식: 404 → NOT_FOUND, 405 → METHOD_NOT_ALLOWED (StarletteHTTPException 핸들러)

### 카테고리: 동적 관리 (DB + 어드민 CRUD) — cms 소유
- 게시판마다 다른 분류 체계를 코드 상수가 아닌 DB로 관리. 카테고리 마스터는 cms 도메인(어드민 관리 메타데이터). 프론트는 GET /cms/{section}/categories로 섹션별 필터를 동적 조회
- post_category 테이블(id, section, name, sort_order, is_active, created_at, UNIQUE(section, name)). 갈리는 컬럼 없어 section별 분리 X. api-v1과 DB 공유
- PostCategoryService.get_post_categories(section_path): Section.from_path 검증(대소문자 무시), is_active=true, sort_order 정렬. 응답 PostCategoryResponse { id, name, sortOrder }(sort_order serialization_alias)
- post_category_mapping (N:M): post_id, category_id. pms 소유. 글당 최대 3개 앱 검증
- 작성 시 categoryIds 검증은 pms 책임 → 모놀리식은 post_category 직접 조회(LocalPostCategoryQueryClient), 분리 후 PostCategoryQueryClient를 cms HTTP 호출 구현으로 교체 (카테고리는 거의 정적이라 캐시 가능)
- 어드민 카테고리 삭제 시 기존 글 처리(is_active 비활성 vs 매핑 제거)는 어드민 기능 구현 시 결정

### section은 ENUM (테이블화 보류)
- section은 페이지·라우트·전용필드와 코드에 강하게 결합 → row 추가만으로 동작하는 페이지가 안 생김(categories와 다른 점)
- sections 테이블 대신 Section enum(TECH/INVEST/POLITICS) + VARCHAR(20) 저장(UserType/UserStatus와 동일 패턴). 표시명/테마색 등 메타데이터 욕구 생기면 그때 테이블 도입 검토

### count / comment 경계
- like_count, comment_count는 현재 post 컬럼 동기 보관. /api/v2/count 경로는 예약만, 추후 count 서비스로 이관
- 댓글은 comment 도메인 예정(/api/v2/comments). community_comments.post_id는 FK 없이 인덱스 + app-level 검증(분리 대비)

### 검색 시스템 (search, 추후 도입) — 번개장터 구조 참고
- pms vs search 호출 기준: 단건 정확 조회·쓰기·"내 것"(개인화+권한) = pms / 공개 다건 탐색(키워드·카테고리·섹션 목록·필터·정렬·집계) = search
- CQRS: pms = 쓰기 원본(MySQL, 권한·개인화) / search = 읽기 최적화(OpenSearch). 동기화는 pms 쓰기 → 변경 이벤트(SQS/Kinesis) → indexer가 MySQL 읽어 문서화 → OpenSearch 인덱싱 (최종 일관성)
- 도입 전략: 초기엔 pms cursor 목록으로 시작 → 검색·복잡 필터·대량 트래픽 필요 시 OpenSearch+indexer 도입(YAGNI). 별도 서비스 예정(yologram-search-api, yologram-search-indexer)
