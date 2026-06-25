# 구현 기능, 설계 근거

> 전 프로젝트(api-v1/api-v2/web-v1/web-v2) 구현 기능과 설계 근거를 이 곳에서 관리. 앞으로 할 일은 todos.md.
> docs는 메인(루트) 에이전트만 갱신. 서브에이전트는 read-only(참고만).

---

## yologram-api-v1 구현 기능 및 설계 근거

구현 완료된 기능과 그 설계 근거를 기록한다. (앞으로 할 일은 todos.md)

### 구현된 기능

#### UMS (회원/인증) — /api/v1/ums
- 회원가입 POST /user/join (이메일 인증 필수)
- 로그인 POST /auth/login, 로그아웃 POST /auth/logout, 토큰 검증 POST /auth/validate-token
- 회원정보 조회 GET /user/me, 수정 PATCH /user/me, 비밀번호 변경 PATCH /user/me/password
- 이메일 인증 POST /auth/email-verification/send·verify (AWS SES)
- 비밀번호 찾기 POST /auth/password-reset/send·verify·confirm
- 회원탈퇴 DELETE /user/me (현재 하드 삭제)

#### CMS (카테고리) — /api/v1/cms
- 카테고리 조회 GET /{section}/categories (is_active=true, sort_order 정렬)

#### PMS (게시글) — /api/v1/pms
- 작성 POST /{section}/posts (인증, categoryIds 1~3 검증)
- 상세 GET /{section}/posts/{id} (공개)
- 목록 GET /{section}/posts (공개, id desc cursor 페이지네이션, categoryId 필터) — 프로젝트 첫 QueryDSL 사용처

#### 인프라/공통
- R/W splitting(MasterSlaveRoutingDataSource), Testcontainers 통합 테스트, Swagger
- Observability: Grafana Cloud OTLP direct push (logs/metrics/traces)

### 설계 근거

#### 도메인 구조 (모듈러 모놀리식 → MSA 대비)
- 단일 서버에서 도메인별 패키지로 분리. 도메인 경계 = 미래 MSA 경계 = API Gateway path prefix
  - UMS→yologram-user-api, PMS→yologram-post-api, CMS→yologram-cms-api, Comment→comment-api, Count→count-api, News→news-api
- 분리 전략: 도메인 간 직접 JOIN·repository 교차 호출 금지
  - 경계 호출은 인터페이스(QueryClient)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: CategoryQueryClient, UserQueryClient)
  - self HTTP 호출 지양. 모놀리식 단계는 직접 호출 구현
  - 경계 넘는 FK 지양(같은 도메인 내부만 FK). 경계 넘는 참조는 인덱스 + app-level 검증
  - 경계 넘는 동기 트랜잭션 의존 최소화 (count 갱신은 추후 이벤트/최종일관성)

#### API 경로 규칙
- /api/v1/ums/ (유저), /api/v1/pms/{section}/ (게시글), /api/v1/cms/{section}/categories (카테고리)
- /api/v1/comments/ (댓글, 추후), /api/v1/count/ (카운트, 예약), /api/v1/news/ (뉴스, 추후)
- 어드민: 도메인 경로 뒤 admin 세그먼트 → /api/v1/{domain}/admin/... (게이트웨이 라우팅과 일관)

#### 유저 타입
- DEFAULT(일반), POLITICIAN(정치인), ECONOMIST(경제인), ADMIN(관리자)

#### 인증 방식
- JWT(HMAC256), Authorization: Bearer 헤더
- access token은 stateless (서버 미저장). 다중 로그인 지원 위해 DB 토큰 비교 제거 → 로그아웃은 클라이언트 토큰 폐기, 서버측 강제 무효화는 refresh token 도입 시 함께 구현
- validate-token은 로그인 직후 replica lag 회피 위해 master DB 조회

#### 이메일 인증
- EmailSender 인터페이스로 발송 추상화 (개발: StubEmailSender 로그, 프로덕션: SesEmailSender)
- user_email_verification 테이블(email, code 6자리, verified, expiredAt). 코드 발송→검증(verified=true)→가입 시 확인→가입 후 삭제. 재발송 시 기존 삭제 후 생성

#### 비밀번호 찾기
- 이메일 6자리 코드 발송→검증→재설정 (이메일 인증과 동일 패턴/SES 재사용)
- 별도 테이블 user_password_reset_code (회원가입 인증 로직과 분리해 회귀 위험 최소화)
- send(미가입 404, 기존 코드 삭제 후 발송) → verify(verified=true, 프론트 게이팅) → confirm(email·code·newPassword 재검증 후 변경·코드 삭제)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한 (현재 평문·무제한)

#### 회원탈퇴 데이터 정리 전략 (soft delete 전환 시 결정)
- 탈퇴 요청과 연관 데이터(게시글/댓글/좋아요) 삭제를 분리해 요청 부하 완화
- 1차(동기): status=DELETED + deletedDate 기록, 토큰 무효화 후 204. 조회 시 DELETED 필터링 → 즉시 탈퇴 효과
- 2차(비동기 연관 삭제): SQS 이벤트+워커(권장) / 배치 잡(간단) / 앱 @Async(소규모, 유실 위험)
- 대량 삭제는 청크(LIMIT N) 반복으로 락·replica 지연 완화. 보관 의무 데이터는 익명화·유예기간 검토
- 권장: soft delete 즉시 응답 + SQS 워커 도메인별 청크 삭제 (규모 작으면 배치 잡으로 시작)

#### 커뮤니티 게시글 데이터 모델: 하이브리드 (단일 + section)
- post (단일 테이블 + section 컬럼): 공통 필드(id, section, user_id, title, content, like_count, comment_count, created_at)
  - 섹션별 전용 필수 필드(투자 종목코드/수익률 등)는 1:1 확장 테이블로 분리(예: invest_post_detail), 해당 섹션 구현 시 추가
- 단일 테이블 채택 이유: 댓글/좋아요/저장/신고 등 상호작용이 섹션 무관 동일 → 자식 테이블·로직을 section마다 복제 안 함. 섹션 페이지·내 글·어드민은 모두 WHERE section=?로 처리. "전용 필드만" 갈리니 그 부분만 확장 테이블(하이브리드)
- 성능: 복합 인덱스 (section, id) = idx_post_section_id로 범위 스캔. 페이지네이션은 cursor(keyset), OFFSET 지양. 더 커지면 파티셔닝 검토

#### 커서 페이지네이션: id-only (legacy 방식과 일치)
- id가 auto_increment라 작성순=시간순 → id desc만으로 최신순. (section, id) 인덱스로 필터+정렬+커서를 한 번에 커버
- created_at 복합 커서는 정렬 기준이 id와 어긋날 때(인기순 등)나 정렬 키가 비유니크일 때만 필요 — 단순 최신순 피드엔 과함
- 종료 판정: +1/hasNext/count 없이 "마지막 글 id를 항상 nextCursor로, 빈 결과면 null". 클라이언트(TanStack useInfiniteQuery)는 nextCursor 유무로만 판단

#### 카테고리: 동적 관리 (DB + 어드민 CRUD) — cms 소유
- 게시판마다 다른 분류 체계를 코드 상수가 아닌 DB로 관리. 카테고리 마스터는 cms 도메인(어드민 관리 메타데이터)
- post_category 테이블(id, section, name, sort_order, is_active, created_at, UNIQUE(section, name)). 갈리는 컬럼 없어 section별 분리 X
- post_category_mapping (N:M): post_id, category_id. pms 소유. 글당 최대 3개 앱 검증
- 작성 시 categoryIds 검증은 pms 책임 → 모놀리식은 post_category 직접 조회, 분리 후 PostCategoryQueryClient를 cms HTTP 호출 구현으로 교체 (카테고리는 거의 정적이라 캐시 가능)
- 어드민 카테고리 삭제 시 기존 글 처리(is_active 비활성 vs 매핑 제거)는 어드민 기능 구현 시 결정

#### section은 ENUM (테이블화 보류)
- section은 페이지·라우트·전용필드와 코드에 강하게 결합 → row 추가만으로 동작하는 페이지가 안 생김(categories와 다른 점)
- sections 테이블 대신 Section enum + VARCHAR(20) 저장(UserType/UserStatus와 동일 패턴). 표시명/테마색 등 메타데이터 욕구 생기면 그때 테이블 도입 검토

#### count / comment 경계
- like_count, comment_count는 현재 post 컬럼 동기 보관. /api/v1/count 경로는 예약만, 추후 count 서비스로 이관
- 댓글은 comment 도메인 예정(/api/v1/comments). community_comments.post_id는 FK 없이 인덱스 + app-level 검증(분리 대비)

#### QueryDSL 사용 기준 (api-v1)
- 단순 단건·고정 조건(findBy/existsBy)·기본 CRUD → Spring Data JpaRepository로 충분
- 다음 중 하나라도 해당하면 QueryDSL custom repository: ①동적 조건 ②다중 조인(2개+ 엔티티) ③projection(필요 컬럼만 DTO) ④조건부 정렬/cursor·offset 페이지네이션 ⑤벌크 update·delete
- yologram 예: "내 글 목록"(section·작성자 필터 + 카테고리 조인 + 페이지네이션) = pms + QueryDSL / 공개 섹션 피드·키워드 검색 = search
- 구현 메모: PostRepositoryImpl.findPostsBySection이 첫 QueryDSL 사용처 — 동적 조건(categoryId/cursor) + EXISTS 서브쿼리 + keyset. JPAQueryFactory 빈은 QuerydslConfig
- 함정: enum을 담는 패키지명에 `enum`(Java 예약어) 금지. QueryDSL APT가 `import ...enum.Xxx`를 생성 못 해 해당 enum 필드가 Q클래스에서 통째 누락됨(다른 타입 필드는 정상). cms.enum → cms.enums로 해결. ums.enum도 동일 잠재 이슈(현재 QueryDSL 미사용이라 보류)

#### 검색 시스템 (search, 추후 도입) — 번개장터 구조 참고
- pms vs search 호출 기준: 단건 정확 조회·쓰기·"내 것"(개인화+권한) = pms / 공개 다건 탐색(키워드·카테고리·섹션 목록·필터·정렬·집계) = search
- CQRS: pms = 쓰기 원본(MySQL, 권한·개인화) / search = 읽기 최적화(OpenSearch). 동기화는 pms 쓰기 → 변경 이벤트(SQS/Kinesis) → indexer가 MySQL 읽어 문서화 → OpenSearch 인덱싱 (최종 일관성)
- QueryDSL vs search 역할: QueryDSL은 관계형 복잡성(권한 한정 "내 것/정확"), search는 탐색 복잡성(풀텍스트·연관도·패싯, 공개 카탈로그 발견)
- 도입 전략: 초기엔 pms cursor 목록으로 시작 → 검색·복잡 필터·대량 트래픽 필요 시 OpenSearch+indexer 도입(YAGNI). 별도 서비스 예정(yologram-search-api, yologram-search-indexer)

---

## yologram-api-v2 구현 기능 및 설계 근거

구현 완료된 기능과 그 설계 근거를 기록한다. (앞으로 할 일은 todos.md)

api-v1(Spring Boot)을 FastAPI로 미러링한 프로젝트로, 기능·설계가 거의 동일하다. 두 프로젝트 비교분석을 통한 학습이 목적이며 객체지향 설계·Layered Architecture·의존성 주입을 적용한다.

### 구현된 기능

#### 기반/공통
- 프로젝트 구성: uv init, 디렉토리 구조(app/config, app/domain, app/core), .env/.env.prod/.env.staging
- 설정: pydantic BaseSettings(Settings), APP_PROFILE로 .env.prod/.env.staging 분기
- DB: SQLAlchemy + PyMySQL, engine + SessionLocal, get_db 의존성 (app/config/database.py)
- 응답 래퍼 ApiEnvelop ({ "data": T })
- 예외 처리 (AppException → { errorMessage, errorCode })
- CORS 전체 허용 (*)
- 컨테이너화: Dockerfile(python:3.12-slim multi-stage), GitHub Actions(ECR push → ECS 재배포)

#### Test 도메인 — /api/v2/test
- GET /api/v2/test (기본 응답), /echo(요청 정보), /profile(활성 프로파일), /property?key=...(설정값 조회)

#### UMS (회원/인증) — /api/v2/ums
- 회원가입 POST /user/join (이메일 인증 필수)
- 로그인 POST /auth/login, 로그아웃 POST /auth/logout (204), 토큰 검증 POST /auth/validate-token
- 회원정보 조회 GET /user/me, 수정 PATCH /user/me(이름·닉네임), 비밀번호 변경 PATCH /user/me/password
- 이메일 인증 POST /auth/email-verification/send·verify (AWS SES)
- 비밀번호 찾기 POST /auth/password-reset/send·verify·confirm
- 회원탈퇴 DELETE /user/me (현재 하드 삭제)

#### CMS (카테고리) — /api/v2/cms
- 카테고리 조회 GET /{section}/categories (is_active=true, sort_order 정렬)

#### PMS (게시글) — /api/v2/pms
- 작성 POST /{section}/posts (인증, categoryIds 1~3 검증)
- 상세 GET /{section}/posts/{id} (공개)
- 목록 GET /{section}/posts (공개, id desc cursor 페이지네이션, categoryId 필터)

#### 인프라/공통
- Observability: Grafana Cloud OTLP direct push (logs/metrics/traces)
  - Logs: LoggerProvider + OTLPLogExporter (app/config/logging.py)
  - Metrics: MeterProvider + OTLPMetricExporter + SystemMetricsInstrumentor (app/config/metrics.py)
  - Traces: TracerProvider + OTLPSpanExporter + FastAPIInstrumentor (app/config/tracing.py)
  - Resource 속성: service.name, deployment.environment.name, service.instance.id, service.namespace
- Swagger /api/v2/docs

### Spring Boot(v1) ↔ FastAPI(v2) 대응 구조

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

### 설계 근거

#### 설정 관리
- 로컬/일반 설정: .env 파일. pydantic-settings BaseSettings가 .env 기본 지원, APP_PROFILE로 분기. 12-Factor App·Python 생태계 보편 방식
- Secret: AWS Parameter Store 저장 → ECS Task Definition secrets 블록으로 컨테이너 환경변수 주입. 코드는 os.environ 접근(프레임워크 의존 없음)
- Spring Boot는 프레임워크가 Parameter Store를 직접 통합하지만, FastAPI(및 대부분 프레임워크)는 인프라(ECS/K8s)에 secret 주입을 맡기는 게 표준

#### 결정 사항
- 패키지 매니저: uv
- 설정 관리: pydantic-settings + .env (YAML 아님)
- Secret: ECS Task Definition secrets (앱 코드에서 직접 안 가져옴)
- Python 3.12+

#### 도메인 구조 (모듈러 모놀리식 → MSA 대비)
- 단일 서버에서 도메인별 패키지로 분리. 도메인 경계 = 미래 MSA 경계 = API Gateway path prefix
  - UMS→yologram-user-api, PMS→yologram-post-api, CMS→yologram-cms-api, Comment→comment-api, Count→count-api, News→news-api
- 분리 전략: 도메인 간 직접 JOIN·repository 교차 호출 금지
  - 경계 호출은 인터페이스(QueryClient/Protocol)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: PostCategoryQueryClient, UserQueryClient)
  - self HTTP 호출 지양. 모놀리식 단계는 직접 호출(Local*QueryClient) 구현
  - 경계 넘는 FK 지양(같은 도메인 내부만 FK). 경계 넘는 참조는 인덱스 + app-level 검증
  - 경계 넘는 동기 트랜잭션 의존 최소화 (count 갱신은 추후 이벤트/최종일관성)
- 도메인 분리(pms/cms/comment/count/news)·하이브리드 스키마 결정은 api-v1 docs/brainstorm.md에 상세. api-v2는 동일 설계 미러링

#### API 경로 규칙
- /api/v2/ums/ (유저), /api/v2/pms/{section}/ (게시글), /api/v2/cms/{section}/categories (카테고리)
- /api/v2/comments/ (댓글, 추후), /api/v2/count/ (카운트, 예약), /api/v2/news/ (뉴스, 추후)
- 어드민: 도메인 경로 뒤 admin 세그먼트 → /api/v2/{domain}/admin/... (게이트웨이 라우팅과 일관)

#### 유저 타입
- DEFAULT(일반), POLITICIAN(정치인), ECONOMIST(경제인), ADMIN(관리자) — UserType/UserStatus enum (VARCHAR 저장)

#### 인증 방식
- JWT(HMAC256, PyJWT), Authorization: Bearer 헤더. api-v1과 동일한 secret/issuer/audience
- 설정: jwt_secret(환경변수 JWT_SECRET, Parameter Store), jwt_expire(86400), jwt_issuer(yologram.link), jwt_audience(yologram.client)
- jwt_util.py: create_token(uid), validate_and_get_uid(token)
- get_authenticated_user 의존성(Bearer 헤더 → JWT 검증 → AuthData)
- access token은 stateless (서버 미저장). 다중 로그인 지원 위해 DB 토큰 비교 제거 → 로그아웃은 클라이언트 토큰 폐기, 서버측 강제 무효화는 refresh token 도입 시 함께 구현
- validate-token은 JWT 서명/만료 검증 + 사용자 존재 확인
- 예외: AuthWrongPasswordException(401), AuthTokenExpiredException(401), AuthTokenInvalidException(401)

#### 이메일 인증
- EmailSender 프로토콜로 발송 추상화 (개발: StubEmailSender 로그, 프로덕션: SesEmailSender boto3). get_email_sender 의존성이 프로파일에 따라 주입
- 발신 주소 no-reply@yologram.link (ses_from_address), 리전 ap-northeast-2. 자격증명: prod ECS Task Role, 로컬 AWS_PROFILE(scripts/run-prod.sh)
- UserEmailVerification 테이블(email, code 6자리, verified, expired_at 5분, created_at). 코드 발송→검증(verified=true)→가입 시 확인→가입 후 삭제. 재발송 시 기존 삭제 후 생성

#### 비밀번호 찾기
- 이메일 6자리 코드 발송→검증→재설정 (이메일 인증과 동일 패턴/SES 재사용, api-v1과 동일)
- 별도 테이블 user_password_reset_code (회원가입 인증 로직과 분리해 회귀 위험 최소화, api-v1과 공유). UserPasswordResetCode 모델: email, code, verified, expired_at 5분, created_at
- send(미가입 404 USER_NOT_FOUND, 기존 코드 삭제 후 발송) → verify(verified=true, 프론트 게이팅) → confirm(email·code·newPassword 재검증 후 변경·코드 삭제)
- 예외: UserPasswordResetExpiredException/UserPasswordResetInvalidException (400)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한 (현재 평문·무제한)

#### 회원탈퇴 데이터 정리 전략 (soft delete 전환 시 결정)
- 현재(개발 단계): 레코드 즉시 하드 삭제(UserService.withdraw) → email 해제로 재가입 가능 (api-v1과 동일)
- 탈퇴 요청과 연관 데이터(게시글/댓글/좋아요) 삭제를 분리해 요청 부하 완화
- 1차(동기, 즉시 응답): status=DELETED + deleted_date 기록, access_token 무효화 후 204. 조회 시 DELETED 필터링 → 즉시 탈퇴 효과
- 2차(비동기 연관 삭제) 옵션:
  - SQS 이벤트 + 워커 (권장, 확장성): "user-deleted" 발행 → 워커가 도메인별 배치 삭제, 실패 시 DLQ 재시도
  - 배치 잡 (간단): DELETED 유저를 주기 스캔해 청크 삭제, 별도 인프라 최소
  - 앱 내 BackgroundTasks (소규모/임시): 요청과 분리하나 인스턴스 종속 → 배포/장애 시 유실 위험, 대량 부적합
- 대량 삭제는 청크(LIMIT N) 단위 반복으로 락/replica 지연 완화. 보관 의무 데이터는 익명화·유예기간(복구) 검토(soft delete면 자연 지원)
- 권장: soft delete 즉시 응답 + SQS 워커 도메인별 청크 삭제 (규모 작으면 배치 잡으로 시작)

#### 커뮤니티 게시글 데이터 모델: 하이브리드 (단일 + section)
- post (단일 테이블 + section 컬럼): 공통 필드(id, section, user_id, title, content, like_count, comment_count, created_at)
  - 섹션별 전용 필수 필드(투자 종목코드/수익률 등)는 1:1 확장 테이블로 분리(예: invest_post_detail), 해당 섹션 구현 시 추가
- 단일 테이블 채택 이유: 댓글/좋아요/저장/신고 등 상호작용이 섹션 무관 동일 → 자식 테이블·로직을 section마다 복제 안 함. 섹션 페이지·내 글·어드민은 모두 WHERE section=?로 처리. "전용 필드만" 갈리니 그 부분만 확장 테이블(하이브리드)
- 성능: 복합 인덱스 (section, id) = idx_post_section_id로 범위 스캔. 페이지네이션은 cursor(keyset), OFFSET 지양. 더 커지면 파티셔닝 검토
- post / post_category_mapping 테이블은 api-v1과 DB 공유. 경계 넘는 참조(user_id, category_id)는 FK 없이 인덱스. Post/PostCategoryMapping 모델, PostRepository/PostCategoryMappingRepository

#### 커서 페이지네이션: id-only (legacy 방식과 일치)
- id가 auto_increment라 작성순=시간순 → id desc만으로 최신순. (section, id) 인덱스로 필터+정렬+커서를 한 번에 커버
- created_at 복합 커서는 정렬 기준이 id와 어긋날 때(인기순 등)나 정렬 키가 비유니크일 때만 필요 — 단순 최신순 피드엔 과함
- 종료 판정: +1/hasNext/count 없이 "마지막 글 id를 항상 nextCursor로, 빈 결과면 null". 클라이언트(TanStack useInfiniteQuery)는 nextCursor 유무로만 판단
- 구현: ApiEnvelopCursorPage[T]{data, nextCursor} / PostSummaryResponse(content 전체 포함) / PostCursor(id-only Base64). 잘못된 커서 → 400 INVALID_CURSOR
- N+1 회피: find_nicknames(UserRepository.find_by_ids, ums 경계 추상화)·find_by_post_ids 배치 조회. 카테고리는 1:N이라 join 대신 IN, categoryId 필터는 EXISTS

#### 게시글 상세 조회
- GET /api/v2/pms/{section}/posts/{id} (공개). PostDetailResponse에 author{uid, nickname} 포함(UserQueryClient/LocalUserQueryClient로 ums 조회, MSA 대비), categoryIds는 프론트가 매핑
- PostRepository.find_by_id, PostCategoryRepository.find_by_post_id. 없거나 다른 section의 id면 404 POST_NOT_FOUND (PostNotFoundException)

#### 게시글 작성 검증
- PostService.create: 작성자=인증 유저(uid), categoryIds가 해당 section 활성 카테고리인지 검증(1~3개 필수). 프론트도 카테고리 1개 이상 선택해야 작성 가능(미선택 시 버튼 비활성)
- 요청 { title?, content, categoryIds[] }, 응답 { id } (201)
- 예외: InvalidPostCategoryException(400 INVALID_POST_CATEGORY), 잘못된 section은 InvalidSectionException(400)
- 검증 메시지는 api-v1과 동일 문구 ("내용을 입력해주세요.", "카테고리는 1~3개 선택해주세요.")

#### 검증 응답 통일 (api-v1 정합)
- RequestValidationError 핸들러: status 422 → 400, errorCode VALIDATION_ERROR, 메시지는 첫 에러를 사람이 읽을 단일 문자열로(Pydantic "Value error, " 접두 제거)
- 라우팅 예외도 동일 형식: 404 → NOT_FOUND, 405 → METHOD_NOT_ALLOWED (StarletteHTTPException 핸들러)

#### 카테고리: 동적 관리 (DB + 어드민 CRUD) — cms 소유
- 게시판마다 다른 분류 체계를 코드 상수가 아닌 DB로 관리. 카테고리 마스터는 cms 도메인(어드민 관리 메타데이터). 프론트는 GET /cms/{section}/categories로 섹션별 필터를 동적 조회
- post_category 테이블(id, section, name, sort_order, is_active, created_at, UNIQUE(section, name)). 갈리는 컬럼 없어 section별 분리 X. api-v1과 DB 공유
- PostCategoryService.get_post_categories(section_path): Section.from_path 검증(대소문자 무시), is_active=true, sort_order 정렬. 응답 PostCategoryResponse { id, name, sortOrder }(sort_order serialization_alias)
- post_category_mapping (N:M): post_id, category_id. pms 소유. 글당 최대 3개 앱 검증
- 작성 시 categoryIds 검증은 pms 책임 → 모놀리식은 post_category 직접 조회(LocalPostCategoryQueryClient), 분리 후 PostCategoryQueryClient를 cms HTTP 호출 구현으로 교체 (카테고리는 거의 정적이라 캐시 가능)
- 어드민 카테고리 삭제 시 기존 글 처리(is_active 비활성 vs 매핑 제거)는 어드민 기능 구현 시 결정

#### section은 ENUM (테이블화 보류)
- section은 페이지·라우트·전용필드와 코드에 강하게 결합 → row 추가만으로 동작하는 페이지가 안 생김(categories와 다른 점)
- sections 테이블 대신 Section enum(TECH/INVEST/POLITICS) + VARCHAR(20) 저장(UserType/UserStatus와 동일 패턴). 표시명/테마색 등 메타데이터 욕구 생기면 그때 테이블 도입 검토

#### count / comment 경계
- like_count, comment_count는 현재 post 컬럼 동기 보관. /api/v2/count 경로는 예약만, 추후 count 서비스로 이관
- 댓글은 comment 도메인 예정(/api/v2/comments). community_comments.post_id는 FK 없이 인덱스 + app-level 검증(분리 대비)

#### 검색 시스템 (search, 추후 도입) — 번개장터 구조 참고
- pms vs search 호출 기준: 단건 정확 조회·쓰기·"내 것"(개인화+권한) = pms / 공개 다건 탐색(키워드·카테고리·섹션 목록·필터·정렬·집계) = search
- CQRS: pms = 쓰기 원본(MySQL, 권한·개인화) / search = 읽기 최적화(OpenSearch). 동기화는 pms 쓰기 → 변경 이벤트(SQS/Kinesis) → indexer가 MySQL 읽어 문서화 → OpenSearch 인덱싱 (최종 일관성)
- 도입 전략: 초기엔 pms cursor 목록으로 시작 → 검색·복잡 필터·대량 트래픽 필요 시 OpenSearch+indexer 도입(YAGNI). 별도 서비스 예정(yologram-search-api, yologram-search-indexer)

---

## yologram-web-v1 구현 기능 및 설계 근거

구현 완료된 화면/기능과 그 설계·UX 근거를 기록한다. (앞으로 할 일은 todos.md)

React 프론트(CSR) 관점에서 화면·컴포넌트·상태관리·UX를 다룬다.

### 프로젝트 목적

- yologram-web-v2(Next.js)와 동일한 기능을 React로 구현
- React vs Next.js 비교 학습용 토이프로젝트

### 기술 스택

- React 19, React Router 7, TypeScript
- Vite 빌드, Yarn Berry(non-zero-install), Node 24
- Ant Design UI + CSS Modules(커스텀 스타일)
- Jotai(상태 관리), axios + TanStack Query(API 통신)

### 구현된 기능

#### 레이아웃/네비게이션
- 반응형 레이아웃: 모바일 탭바 + 데스크탑 사이드바 (ResponsiveLayout, useIsMobile 768px 분기)
- 5개 최상위 탭: /invest, /politics, /tech, /notifications, /settings (/ → /invest 리다이렉트)
- 최상위 탭 순서 기술 우선
- 기술 페이지 서브탭: 커뮤니티·채용 (SubTabLayout)
- 데스크탑 본문 760px 중앙 고정

#### 인증 (UMS 연동)
- 회원가입: JoinPage 단계적 폼(이메일 인증 → 이름·닉네임·비밀번호) → POST /api/v1/ums/user/join
- 로그인/로그아웃: 실제 API 호출 (POST /auth/login·logout)
- 토큰 검증: validateToken() (POST /auth/validate-token)
- 이메일 인증: 코드 발송/검증 (POST /auth/email-verification/send·verify)
- 비밀번호 찾기: ForgotPasswordPage 단계적 폼(이메일 → 코드 발송/검증 → 새 비밀번호), apis/auth.ts send/verify/confirm + 뮤테이션 훅 3개
- AuthGate: 앱 시작 시 저장 토큰 검증 후 라우터 렌더링, authAtom localStorage rehydrate

#### 설정
- 회원정보 조회: GET /api/v1/ums/user/me (useUserQuery), 설정 페이지 아바타 하단 닉네임 표시
- 회원정보 수정: EditProfilePage(이메일·이름 읽기전용, 닉네임 변경) → PATCH /user/me (useUpdateProfileMutation), 성공 시 설정 이동 + user 쿼리 무효화
- 비밀번호 변경: 현재/새/확인 입력 → PATCH /user/me/password (useChangePasswordMutation)
- 회원탈퇴: 확인 모달 → DELETE /user/me (useWithdrawMutation), 성공 시 localStorage('auth') 제거 + /login 이동
- 활동 - 내가 쓴 글: /settings/my-posts (게시판 필터 기술/투자/정치, 현재 더미)

#### 기술 커뮤니티 피드 (백엔드 연동)
- 목록 API GET /api/v1/pms/{section}/posts cursor 무한스크롤(useInfiniteQuery, nextCursor 기준)
- categoryId 서버 필터
- PostCard를 PostSummary 기반·상대시간(lib/date.formatRelativeTime)으로 전환
- 작성 후 invalidate, 피드 더미 atom 제거(내 글 더미만 유지)

#### 공통/상태관리
- 전역 상태 관리 Jotai(authAtom)
- 입력 폼 유효성 통과 시 제출 버튼 활성화 (로그인/회원가입/비밀번호 변경/회원정보 수정)

#### 개발 환경
- 테스트 환경: vitest + @testing-library/react + msw
- 테스트 유틸리티: src/test/ (setup.ts, handlers.ts, server.ts, utils.tsx)
- 작성된 테스트: JoinPage, LoginPage, EditProfilePage, ForgotPasswordPage, auth.test.ts (로그인/로그아웃/이메일 인증/비밀번호 찾기)

### 설계 근거

#### React vs Next.js 비교 포인트 (학습 목적)
- 라우팅: React Router(코드 기반) vs App Router(파일 기반)
- 환경 변수: VITE_ prefix vs NEXT_PUBLIC_
- 빌드: Vite → 정적 파일 vs Next.js → standalone Node 서버
- 배포: nginx/정적 서빙(S3+CloudFront) vs node server.js
- SSR: React는 CSR only, Next.js는 SSR/SSG 가능

#### 환경 분리
- .env.development(로컬), .env.staging(스테이징), .env.production(프로덕션)
- Vite 빌트인 모드로 분리(--mode staging, --mode production)
- 환경 변수 prefix VITE_ (Next.js NEXT_PUBLIC_ 대응)
- 로컬 포트 3001, 로컬 API URL http://localhost:5001

#### Dockerfile/배포
- multi-stage 빌드(의존성 설치 + 빌드 → nginx로 정적 파일 서빙)
- React SPA 빌드 결과물은 정적 파일이라 Next.js(standalone node)와 다름
- 빌드 시 --mode arg로 환경 지정
- 배포: S3 + CloudFront, build/ 결과물 S3 sync, index.html no-cache·나머지 1년 캐시(Vite 해시 파일명)

#### 인증 상태/게이팅
- JWT는 Jotai authAtom + localStorage 저장, AuthState에 name 필드 포함(API 응답)
- AuthGate가 앱 시작 시 저장 토큰을 validate-token으로 검증 후 라우터 렌더링
- RequireAuth는 인증 초기화 완료 후 보호 라우트 진입 여부만 판단
- 401 인터셉터에서 authAtom 초기화(토큰 만료/무효 시 자동 로그아웃)
- 새 탭/새로고침 시 authAtom rehydrate 보강

#### 프론트 유효성/UX
- 프론트 validation은 서버와 동일(이메일 형식, 이름/닉네임 2~20자, 비밀번호 8~20자)
- 이메일 인증 완료 전 회원가입 버튼 비활성(EMAIL_NOT_VERIFIED 사전 차단), 이메일 변경 시 인증 상태 초기화·재발송 지원
- 비밀번호 찾기 폼 유효성 게이팅(코드 발송/인증/변경 버튼), 이메일 변경 시 단계 초기화
- 입력 폼 제출 버튼은 클라이언트 유효성 통과 시에만 활성화(Ant Form은 useFormSubmittable 훅, 수동 상태 폼은 파생 isValid)

#### 회원탈퇴 UX
- 백엔드가 개발 단계 하드 삭제라 탈퇴 후 같은 이메일 재가입 가능

#### 피드 연동 설계
- cursor 무한스크롤은 백엔드 id-only 커서 방식과 일치(nextCursor 유무로만 다음 페이지 판단)
- PostCard는 PostSummary 기반, 상대시간 표시(lib/date.formatRelativeTime)

#### 페이지 구성 규칙
- 페이지별 디렉토리 관리(pages/invest/, pages/politics/ 등)
- CSS는 컴포넌트와 같은 디렉토리에 .module.css 배치, 글로벌 스타일만 styles/ 분리
- UI는 Ant Design 우선, 커스텀 필요 시 CSS Modules

---

## yologram-web-v2 구현 기능 및 설계 근거

구현 완료된 기능과 그 설계/UX 근거를 기록한다. (앞으로 할 일은 todos.md)

Next.js(App Router) 프론트 관점 — 화면/라우팅/서버·클라이언트 컴포넌트/상태관리/UX 위주.

### 기술 스택

- Next.js (App Router) — 레거시(web-v1)는 React + Vite, Next.js로 전환
- TypeScript
- Ant Design — 레거시와 동일
- Emotion — 레거시와 동일
- TanStack Query — 레거시와 동일
- Jotai — 레거시와 동일
- axios — 레거시와 동일
- Docker (Yarn Berry non-zero-install, 일반 next start 방식)
- 패키지 매니저: yarn (레거시와 동일)

#### React → Next.js 전환 시 차이점
- 라우팅: React Router → App Router (파일 기반)
- 환경 변수 prefix: VITE_ → NEXT_PUBLIC_
- 빌드: Vite → Next.js 빌드
- SSR/SSG 활용 가능

#### src/ 디렉토리 구조 (레거시 참고)
- app/ : Next.js App Router 페이지 (레거시 pages/ 대응)
- apis/ : API 통신
- components/ : 공통 컴포넌트
- hooks/ : 커스텀 훅
- queries/ : TanStack Query 훅
- stores/ : Jotai 상태
- styles/ : 스타일
- types/ : 타입 정의
- utils/ : 유틸리티

### 구현된 기능

#### 인증 (로그인/로그아웃/토큰검증)
- 로그인 POST /api/v2/ums/auth/login, 로그아웃 POST /api/v2/ums/auth/logout, 토큰검증 POST /api/v2/ums/auth/validate-token
- AuthState에 name 필드 추가, atomWithStorage에 getOnInit: true 적용
- 401 인터셉터: /ums/auth/ URL 제외, redirect 제거(authAtom 초기화만)
- AuthGate 컴포넌트로 앱 마운트 시 저장 토큰 검증 후 렌더링
- lib/error.ts(getErrorMessage)로 네트워크/서버/비즈니스 에러 분류
- 로그아웃: localStorage.removeItem + window.location.href 방식

#### 회원가입 + 이메일 인증
- 회원가입 POST /api/v2/ums/user/join
- 단계적 폼: 이메일 입력 → 인증코드 발송 → 코드 입력/검증 → 인증 완료 시 이름·닉네임·비밀번호·회원가입 활성화
- apis/auth.ts: sendVerificationCode, verifyEmail (POST /api/v2/ums/auth/email-verification/send·verify)
- 뮤테이션: useSendVerificationCodeMutation, useVerifyEmailMutation, useJoinMutation
- 이메일 변경 시 인증 상태 초기화, 재발송 지원
- 회원가입 버튼은 인증 완료 전 비활성 (EMAIL_NOT_VERIFIED 사전 차단)

#### 비밀번호 찾기
- 로그인 페이지에 "비밀번호를 잊으셨나요?" 링크 (/forgot-password)
- 단계적 폼: 이메일 → 코드 발송 → 코드 검증 → 새 비밀번호 설정
- apis/auth.ts: sendPasswordResetCode, verifyPasswordResetCode, confirmPasswordReset
- 뮤테이션: useSendPasswordResetCodeMutation, useVerifyPasswordResetCodeMutation, useConfirmPasswordResetMutation
- 폼 유효성 게이팅, 이메일 변경 시 단계 초기화, 성공 시 로그인 이동

#### 설정 - 회원정보 조회
- GET /api/v2/ums/user/me 연동 (useUserQuery)
- 설정 페이지 아바타 하단에 닉네임 표시

#### 설정 - 회원정보 수정
- 회원정보 수정 페이지 (이메일/이름 읽기전용, 닉네임 변경 폼)
- PATCH /api/v2/ums/user/me 연동
- 수정 성공 시 설정 페이지 이동 + 닉네임 갱신

#### 설정 - 비밀번호 변경
- 비밀번호 변경 페이지 (현재/새/확인 입력)
- PATCH /api/v2/ums/user/me/password 연동 (useChangePasswordMutation)

#### 설정 - 회원탈퇴
- 설정 페이지 회원탈퇴 버튼 → 확인 모달 → DELETE /api/v2/ums/user/me (useWithdrawMutation)
- 성공 시 localStorage('auth') 제거 + /login 이동
- 백엔드가 개발 단계 하드 삭제라 탈퇴 후 같은 이메일 재가입 가능

#### 기술 커뮤니티 (피드/작성/상세)
- 기술 서브탭에 커뮤니티·채용 추가, 최상위 탭 순서 변경(기술 우선) + 기본 진입 /tech
- 피드 (/tech/community): PostCard 목록 + 무한 스크롤 + 하단 작성바 + 맨 위로 FAB
- 글 작성 (/tech/community/write): 제목(optional) + 내용 + 카테고리(최대3) + 풀스크린 오버레이
- 글 상세 (/tech/community/[postId]): 본문 + 액션행 + 댓글 목록/입력 + 풀스크린 오버레이
- 카테고리: 필터(전체+7) 단일선택 / 작성 다중 태깅(최대3) / 배지
- 기술 서브탭 헤더 스크롤 collapse (SubTabLayout collapseOnScroll)
- 피드 백엔드 연동: 목록 API(GET /api/v2/pms/{section}/posts) cursor 무한스크롤(useInfiniteQuery, nextCursor 기준), categoryId 서버 필터, PostCard를 PostSummary 기반·상대시간(lib/date.formatRelativeTime)으로 전환, 작성 후 invalidate, 피드 더미 atom 제거(내 글 더미만 유지) — web-v1과 동일
- 게시글/댓글 타입 + Jotai atom(더미 시드, 내 글 더미만 유지)
- web-v1과 동일 기능 (라우팅만 App Router 방식, 작성/상세는 fixed 오버레이로 전체화면)

#### 공통 UI/UX
- 입력 폼 유효성 통과 시에만 제출 버튼 활성화 (로그인/회원가입/비밀번호 변경/회원정보 수정)
- antd App 래퍼 적용(모달/메시지 테마 일관)
- 링크/활성 칩 분홍 테마(#f2a0b5) 통일, 필터 칩 가로 스크롤

#### 개발 환경 / 테스트
- 테스트 환경 구성: vitest + @testing-library/react + msw
- MSW 핸들러(login/validate-token/logout 등) + 인증/페이지 테스트(auth.test.ts, LoginPage.test.tsx, page.test.tsx, forgot-password/page.test.tsx)
- 피드/작성/상세 테스트

#### Observability (server-side trace + metrics)
- Next.js instrumentation 엔트리 추가 (src/instrumentation.ts)
- Node runtime tracing 초기화 (src/instrumentation.node.ts) — Node runtime에서만 SDK 초기화
- Grafana Cloud OTLP trace export 의존성 추가, direct push
- 환경변수 체계 정리: APP_ENV는 런타임 주입, NEXT_PUBLIC_APP_ENV는 .env 유지로 역할 분리, OTEL_EXPORTER_OTLP_*
- Docker 빌드를 Yarn Berry non-zero-install 기준으로 수정, Next.js standalone 출력 제거

### 설계 근거

#### 환경 분리
- .env.development(로컬), .env.staging(스테이징), .env.production(프로덕션)
- 환경 변수 prefix: NEXT_PUBLIC_ (레거시는 VITE_)
- 주요 변수: NEXT_PUBLIC_APP_ENV, API URL, AUTH TOKEN KEY
- 서버 런타임 env(APP_ENV)는 .env가 아니라 실행 주체(package script, ECS)가 주입
- 환경명은 APP_ENV 우선, 없으면 NEXT_PUBLIC_APP_ENV fallback

#### 페이지 구성
- / : 메인 페이지
- /test : 테스트 페이지

#### Dockerfile
- multi-stage 빌드 (의존성 설치 + 빌드 → 실행)
- Yarn Berry는 사용하되 zero-install은 사용하지 않음 (빌드 컨테이너 내부에서 yarn install --immutable)
- Next.js는 일반 next start 방식으로 실행 (standalone 미사용)
- 빌드 시 ENV arg로 환경 지정, 포트 3000

#### 인증 설계 (web-v1 이슈 반영)
- atomWithStorage getOnInit: true → 새 탭/새로고침 시 null 방지
- AuthGate 패턴 → 앱 마운트 시 저장된 토큰 검증 후 렌더링
- 401 인터셉터에서 /ums/auth/ URL 제외 → 로그인 실패 시 auth 초기화 방지
- 401 인터셉터에서 redirect 제거 → authAtom 초기화만
- 로그아웃: localStorage.removeItem + window.location.href → RequireAuth 타이밍 이슈 회피
- Next.js 차이점
  - AuthGate를 providers.tsx에서 래핑 (web-v1은 BrowserRouter 밖)
  - RequireAuth는 children prop 기반 (web-v1은 Outlet 기반)
  - window 접근은 'use client' 컴포넌트에서만 가능

#### Observability 설계
- 1차는 server-side trace만 적용 (확장으로 metrics 추가)
- src/instrumentation.ts에서 Next.js instrumentation register, Node runtime에서만 OpenTelemetry SDK 초기화
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS 기반 exporter 구성 (trace, metrics 공유). endpoint 없으면 setup 생략
- Trace: Next.js App Router 요청/렌더링/fetch span 수집 → Grafana Cloud OTLP direct push
- Metrics: NodeSDK 직접 구성으로 direct push
  - 프로세스 메트릭: @opentelemetry/host-metrics (process.cpu.utilization, process.memory.usage)
  - HTTP 메트릭: @opentelemetry/instrumentation-http 추가했으나 Next.js 환경에서 http.server.request.duration 미생성 확인(한계). 요청 수는 현재 trace 기반으로 확인
- Docker 빌드는 Yarn Berry non-zero-install 기준 유지, Next.js는 standalone 없이 일반 서버 실행
- 서버 런타임 env(APP_ENV)와 public env(NEXT_PUBLIC_APP_ENV) 분리 관리
- production 장기 권장안은 Alloy지만 현재는 direct push
