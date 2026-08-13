# yologram-api-v2 프로젝트 지침

## 프로젝트 개요

FastAPI 기반 API 서버. ECS Fargate에서 운영.

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- app/main.py: 앱 진입점 (logging, metrics, tracing 초기화)
- app/config/settings.py: Pydantic Settings (환경변수 매핑)
- app/config/database.py: engine, SessionLocal, get_db
- app/config/logging.py · metrics.py · tracing.py: OTLP 로그/메트릭/트레이스
- app/domain/pms/tech/publisher/event/: 조회 이벤트 발행 (api-v1 미러) — post_view_event.py(계약, api-v1·worker와 문자열 미러) + post_view_event_publisher.py(put_record, PartitionKey=post_id, 예외 삼킴, 클라이언트 lazy 생성). 스위치는 POST_VIEW_PUBLISH_ENABLED/POST_VIEW_PUBLISH_STREAM
- app/infra/client/{ums,cms,pms,comment}: 도메인 간 경계 클라이언트 — {대상도메인}ApiClient(Protocol) + Local 구현 (api-v1 미러)
- app/infra/cache/ + config/redis.py: Valkey 캐시 — cache-aside(UserNicknameCache·TechNewsFirstPageCache), 전 연산 예외 삼킴(DB 폴백), 1초 타임아웃. 키·JSON api-v1과 바이트 호환 — 닉네임 ums:users:v1:nickname:{uid}(TTL 1h), 뉴스 첫 페이지 news:tech:v1:first-page:{category|all}:{size}(TTL 3분, worker UNLINK 무효화·camelCase envelope). 설정 CACHE_REDIS_HOST/PORT(로컬 .env 16379)
- app/domain/ums: AuthService(JWT 로그인/로그아웃/검증), UserService(가입/수정/비번변경/탈퇴), UserEmailVerificationService + EmailSender(Stub/Ses), UserPasswordResetService
- app/domain/ums의 admin_* 세트: 어드민 인증·계정 관리 (admin_schema/admin_jwt_util/admin_auth_dependency/admin_service/admin_router — /api/v2/ums/admin, 생성(항상 role=ADMIN)·로그인·검증·로그아웃·목록(offset 페이지)·삭제(자기 자신·OWNER 금지)·상태 변경(OWNER 전용, INACTIVE 로그인 차단), api-v1 미러)
- app/domain/{pms,cms,comment,news}/tech: 도메인 우선 구조 — pms/tech(TechPostService), cms/tech(TechCategoryService — tech_category 공용 마스터), comment/tech(TechPostCommentService), news/tech(TechNewsService — 공개 조회: 복합 커서·categoryId 필터·라벨 조인. admin_* 세트 AdminTechNewsSourceService — 어드민 소스 CRUD /news/admin/tech/sources)

## 설정 관리

- .env 파일로 로컬 설정
- ECS secrets (Parameter Store)에서 환경변수로 주입
- DB 환경변수: DB_URL, DB_USERNAME, DB_PASSWORD / JWT: JWT_SECRET, ADMIN_JWT_SECRET / 캐시: CACHE_REDIS_HOST(기본 localhost) / 조회 이벤트 발행: POST_VIEW_PUBLISH_ENABLED·POST_VIEW_PUBLISH_STREAM(기본 false·빈 값 = 발행 스킵, prod는 Dockerfile ENV — api-v1 yologram.events.publish.post-view.*와 대칭)
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS는 OpenTelemetry SDK가 자동으로 읽음
- SES 발신 주소: ses_from_address (기본 no-reply@yologram.link)
- 자격증명: prod ECS Task Role, 로컬 AWS_PROFILE (scripts/run-prod.sh에서 export)
- pydantic-settings: 필드명을 대문자 환경변수로 자동 매핑

## API 응답/예외 (코딩 규칙)

- 응답 래퍼 ApiEnvelop ({ "data": T })
- 예외 AppException → { errorMessage, errorCode }
- 입력값 검증 실패(RequestValidationError): 400 VALIDATION_ERROR, 메시지는 첫 에러를 사람이 읽을 단일 문자열로(Pydantic "Value error, " 접두 제거). status·errorCode를 api-v1과 정합(400)
- 라우팅 예외도 동일 형식: 404 NOT_FOUND, 405 METHOD_NOT_ALLOWED (StarletteHTTPException 핸들러)
- CORS: 전체 허용 (*)

## 인증 (코딩 규칙)

- JWT: PyJWT (HMAC256), api-v1과 동일한 secret/issuer/audience. 인증 헤더 Authorization: Bearer {token}
- 설정: jwt_secret(JWT_SECRET), jwt_expire(86400), jwt_issuer(yologram.link), jwt_audience(yologram.client)
- get_authenticated_user 의존성으로 인증 정보 주입 (FastAPI Depends)
- 어드민: admin_user 테이블 + 전용 JWT(admin_jwt_secret — api-v1과 동일 secret 공유, audience yologram.admin). get_authenticated_admin 의존성, 유저↔어드민 토큰 상호 불인정
- access token은 stateless (서버 미저장). 서버측 강제 무효화는 refresh token 도입 시 함께 구현
- (동작·정책은 docs/done.md, docs/todos.md 참조)

## 이메일 인증 / SES (코딩 규칙)

- EmailSender 프로토콜로 발송 추상화: StubEmailSender(app_profile != prod 로그), SesEmailSender(app_profile == prod, boto3). get_email_sender 의존성이 프로파일에 따라 주입
- 발신 주소: no-reply@yologram.link (ses_from_address), 리전 ap-northeast-2
- 자격증명: ECS Task Role (prod), AWS_PROFILE (로컬, scripts/run-prod.sh)
- 비밀번호 찾기도 동일 패턴/SES 재사용 (UserPasswordResetService)

## 커뮤니티 (tech 게시판 코딩 규칙)

- 섹션별 완전 분리: app/domain/{pms,cms,comment}/tech (도메인 우선, 섹션은 하위) — 테이블 tech_post/tech_post_category_mapping/tech_post_comment + tech_category(게시판·뉴스 공용 마스터) + tech_news/tech_news_category_mapping(뉴스 조회 전용) (api-v1과 DB 공유, 전 테이블 무FK, section 컬럼·Section enum 없음). invest/politics는 동일 세트 복제로 추가
- 경계 검증·조회는 ApiClient(Protocol)로 추상화 (LocalUmsApiClient, LocalCmsApiClient, LocalCommentApiClient, LocalPmsApiClient)
- 검증 메시지는 api-v1과 동일 문구 ("내용을 입력해주세요.", "카테고리는 1~3개 선택해주세요.")
- N+1 회피: find_nicknames·find_by_post_ids 배치 조회, categoryId 필터는 EXISTS
- 댓글 수: tech_post_comment_count 매핑(pms 소유 1:1) — insert().on_duplicate_key_update(+1)/가드 UPDATE(-1)만 사용, 댓글 서비스는 PmsApiClient 경유
- 좋아요: tech_post_like 이력(UNIQUE(post_id,uid), 진실) + tech_post_like_count(1:1) — 이력은 insert().prefix_with("IGNORE") 한 문장(멱등, rowcount로 실삽입 분기), 카운트 증감은 이력 변경 행수(1/0)로만. TechPostLikeService(like_service.py), POST/DELETE /pms/tech/posts/{id}/like 멱등 no-op 200
- 카운트 조회: outerjoin+coalesce(0) 이중 조인 프로젝션(TechPostWithCounts). 응답은 metrics: {commentCount, likeCount, likedByMe} 중첩 — likedByMe는 선택 인증(get_optional_authenticated_user — 헤더 없으면 None, 무효 토큰 401) + 이력 exists/IN 배치. tech_post의 comment_count·like_count 컬럼은 사장(매핑 제거 — drop 예정)
- 응답 스키마의 section 필드는 "TECH" 고정. ApiEnvelopCursorPage는 null 커서 필드 생략(v1 @JsonInclude NON_NULL 정합)
- (데이터 모델·엔드포인트·설계 근거는 docs/done.md, 경로 규칙은 docs/rules.md 참조)

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- pytest + TestClient, mock: unittest.mock (MagicMock, patch)
- uv run pytest tests/ -v

## Observability

- Grafana Cloud OTLP direct push. 설정: app/config/logging.py · metrics.py · tracing.py
- Resource 속성: service.name, deployment.environment.name, service.instance.id, service.namespace
- (라이브러리 상세는 README.md 참조)

## Swagger

- Swagger: /api/v2/docs
- 신규 API 추가 시 Swagger 문서화 필수 (요청/응답 스키마, 에러 코드, 인증 여부)

## 포트

- 로컬/ECS 모두 5000

## 배포

- Docker (python:3.12-slim multi-stage)
- ECS Fargate
- GitHub Actions: Docker build → ECR push → ECS 재배포
