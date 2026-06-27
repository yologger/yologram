# 구현 기능, 설계 근거

> 전 프로젝트(api-v1/api-v2/web-v1/web-v2)의 구현 기능과 설계 근거를 한 곳에서 관리. 앞으로 할 일은 todos.md.
> 백엔드는 api-v1(Spring Boot/Kotlin)·api-v2(FastAPI/Python) 미러링, 프론트는 web-v1(React)·web-v2(Next.js) 미러링. 별도 표기 없으면 각 계층 양쪽 공통.
> api-v2는 api-v1을 FastAPI로 미러링한 비교학습용(객체지향·Layered·DI 적용), web-v1은 web-v2와 동일 기능의 React 비교학습용.
> docs/는 메인(루트) 에이전트만 갱신. 서브에이전트는 read-only(참고만).

---

## 구현된 기능 (대략 구현 순서)

### (UMS) 회원가입 + 이메일 인증
- 백엔드: 회원가입 POST /ums/user/join (이메일 인증 필수, UserService.join에서 verified 확인·가입 후 코드 삭제). 이메일 인증 POST /ums/auth/email-verification/send·verify (AWS SES)
- 프론트: 단계적 폼(이메일 입력 → 인증코드 발송 → 코드 입력/검증 → 인증 완료 시 이름·닉네임·비밀번호 입력·가입 활성화). 이메일 변경 시 인증 상태 초기화·재발송, 인증 완료 전 가입 버튼 비활성(EMAIL_NOT_VERIFIED 사전 차단)

### (UMS) 로그인/로그아웃/토큰 검증
- 백엔드: 로그인 POST /ums/auth/login, 로그아웃 POST /ums/auth/logout(api-v2는 204), 토큰 검증 POST /ums/auth/validate-token
- 프론트: 실제 API 연동, AuthGate가 앱 시작/마운트 시 저장 토큰 검증 후 렌더링. lib/error(getErrorMessage)로 네트워크/서버/비즈니스 에러 분류, 401 인터셉터로 authAtom 초기화

### (UMS) 회원정보 조회/수정·비밀번호 변경
- 백엔드: GET /ums/user/me, 수정 PATCH /ums/user/me(이름·닉네임), 비밀번호 변경 PATCH /ums/user/me/password
- 프론트: 설정 페이지 닉네임 표시(useUserQuery), 회원정보 수정(이메일·이름 읽기전용, 닉네임 변경 → 성공 시 설정 이동 + user 쿼리 무효화), 비밀번호 변경(현재/새/확인)

### (UMS) 비밀번호 찾기
- 백엔드: POST /ums/auth/password-reset/send·verify·confirm
- 프론트: 로그인 페이지 "비밀번호를 잊으셨나요?" 링크(/forgot-password), 단계적 폼(이메일 → 코드 발송/검증 → 새 비밀번호), 성공 시 로그인 이동

### (UMS) 회원탈퇴
- 백엔드: DELETE /ums/user/me (현재 개발 단계 하드 삭제, email 즉시 해제되어 재가입 가능)
- 프론트: 설정 페이지 회원탈퇴 버튼 → 확인 모달 → DELETE, 성공 시 localStorage('auth') 제거 + /login 이동

### (CMS) 커뮤니티 카테고리 조회
- 백엔드: GET /cms/{section}/categories (is_active=true, sort_order 정렬)
- 프론트: 섹션별 필터 칩 동적 렌더(전체 + 카테고리), 단일선택 필터 / 작성 시 다중 태깅(최대 3)

### (PMS) 게시글 작성
- 백엔드: POST /pms/{section}/posts (인증, categoryIds 1~3 검증). 요청 { title?, content, categoryIds[] }, 응답 { id } (201)
- 프론트: 작성 페이지(제목 optional + 내용 + 카테고리 최대 3, web-v2는 풀스크린 오버레이). 카테고리 1개 이상 선택해야 작성 가능(미선택 시 버튼 비활성)

### (PMS) 게시글 상세
- 백엔드: GET /pms/{section}/posts/{id} (공개). PostDetailResponse에 author{uid, nickname} 포함(UserQueryClient로 ums 조회), categoryIds는 프론트가 매핑. 없거나 다른 section의 id면 404 POST_NOT_FOUND
- 프론트: 상세 페이지(본문 + 액션행 + 댓글 목록/입력, web-v2는 풀스크린 오버레이)

### (PMS) 게시글 목록 (cursor 무한스크롤)
- 백엔드: GET /pms/{section}/posts (공개). 최신순(id desc) + cursor(keyset) 페이지네이션, categoryId 필터(옵션), size 기본 20·최대 50. 응답 ApiEnvelopCursorPage{data, nextCursor}, PostSummaryResponse(content 전체 포함). 잘못된 커서 → 400 INVALID_CURSOR. api-v1 첫 QueryDSL 사용처
- 프론트: cursor 무한스크롤(useInfiniteQuery, nextCursor 기준) + 하단 작성바 + 맨 위로 FAB. PostCard를 PostSummary 기반·상대시간(lib/date.formatRelativeTime)으로 전환, 작성 후 invalidate, 피드 더미 atom 제거(내 글 더미만 유지)

### (web) 레이아웃/네비게이션
- 반응형 레이아웃: 모바일 탭바 + 데스크탑 사이드바 (ResponsiveLayout, useIsMobile 768px 분기)
- 5개 최상위 탭: /invest, /politics, /tech, /notifications, /settings (기술 우선 순서, 기본 진입 /tech 또는 / → /invest)
- 기술 페이지 서브탭: 커뮤니티·채용 (SubTabLayout, web-v2는 collapseOnScroll)
- 데스크탑 본문 760px 중앙 고정 (사이드바 제외 본문 영역)
- 링크/활성 칩 분홍 테마(#f2a0b5) 통일, 필터 칩 가로 스크롤. 입력 폼 유효성 통과 시에만 제출 버튼 활성화

### (web) 설정 - 활동
- 내가 쓴 글: /settings/my-posts (게시판 필터, 현재 더미 → 내 글 목록 API 연동 예정)

### 네이밍 컨벤션 통일 (전 프로젝트)
- 테이블 단수형 + user_/post_ prefix: user, user_email_verification, user_password_reset_code, post, post_category, post_category_mapping
- 클래스/모델: UserEmailVerification, UserPasswordResetCode, PostCategory, PostCategoryMapping
- errorCode: USER_ prefix, INVALID_POST_CATEGORY, AUTH_INVALID_TOKEN/AUTH_EXPIRED_TOKEN (api-v1·v2·web mock 일치)

### 인프라 / 공통
- Observability: Grafana Cloud OTLP direct push (api-v1/v2 logs·metrics·traces, web-v2 server-side trace + metrics)
- CI/CD: GitHub Actions(ECR push → ECS 재배포), Discord 알림(env + jq로 셸 인젝션 회피)
- 테스트: api-v1 Testcontainers(MySQL) + MockMvc, api-v2 pytest + TestClient + mock, web vitest + Testing Library + msw
- Swagger: api-v1 /api/v1/docs, api-v2 /api/v2/docs
- api-v2 기반: uv, pydantic-settings(APP_PROFILE 분기), SQLAlchemy + PyMySQL, ApiEnvelop 응답 래퍼, AppException, CORS 전체 허용, Dockerfile(python:3.12-slim multi-stage)
- api-v2 Test 도메인 /api/v2/test: 기본 응답·/echo·/profile·/property?key=...
- web-v2 Observability: src/instrumentation.ts(register) + instrumentation.node.ts(Node runtime만 SDK 초기화), OTLP trace/metrics direct push, host-metrics(process.cpu/memory). Docker는 Yarn Berry non-zero-install, Next.js 일반 next start(standalone 미사용)

---

## 설계 근거

### 도메인 구조 (모듈러 모놀리식 → MSA 대비)
- 단일 서버에서 도메인별 패키지로 분리. 도메인 경계 = 미래 MSA 경계 = API Gateway path prefix
  - UMS→yologram-user-api, PMS→yologram-post-api, CMS→yologram-cms-api, Comment→comment-api, Count→count-api, News→news-api
- 분리 전략: 도메인 간 직접 JOIN·repository 교차 호출 금지
  - 경계 호출은 인터페이스(QueryClient/Protocol)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: PostCategoryQueryClient, UserQueryClient). 모놀리식은 Local*QueryClient(리포지토리 직접) 구현, self HTTP 호출 지양
  - 경계 넘는 FK 지양(같은 도메인 내부만 FK). 경계 넘는 참조는 인덱스 + app-level 검증
  - 경계 넘는 동기 트랜잭션 의존 최소화 (count 갱신은 추후 이벤트/최종일관성)

### API 경로 규칙
- /api/{v1|v2}/ums/ (유저), /pms/{section}/ (게시글), /cms/{section}/categories (카테고리)
- /comments/ (댓글, 추후), /count/ (카운트, 예약), /news/ (뉴스, 추후)
- 어드민: 도메인 경로 뒤 admin 세그먼트 → /{domain}/admin/... (게이트웨이 라우팅과 일관)

### 유저 타입
- DEFAULT(일반), POLITICIAN(정치인), ECONOMIST(경제인), ADMIN(관리자) — UserType/UserStatus enum (VARCHAR 저장)

### 인증 방식 (JWT stateless)
- JWT(HMAC256), Authorization: Bearer 헤더. api-v1 Auth0 java-jwt / api-v2 PyJWT, 동일 secret/issuer(yologram.link)/audience(yologram.client)/expire(86400)
- access token은 stateless (서버 미저장). 다중 로그인 지원 위해 DB 토큰 비교 제거 → 로그아웃은 클라이언트 토큰 폐기, 서버측 강제 무효화는 refresh token 도입 시 함께 구현
- validate-token은 JWT 서명/만료 검증 + 사용자 존재 확인. api-v1은 로그인 직후 replica lag 회피 위해 master DB 조회
- api-v1: @AuthenticatedUser + AuthenticatedUserResolver, 인증 예외(AuthToken*)는 GlobalExceptionHandler 전역 처리(ums 외 도메인도 401 보장). api-v2: get_authenticated_user 의존성, 예외 AuthWrongPassword(401)/AuthTokenExpired(401)/AuthTokenInvalid(401)

### 이메일 인증
- EmailSender 인터페이스/프로토콜로 발송 추상화 (개발: StubEmailSender 로그, 프로덕션: SesEmailSender — api-v1 SDK v2, api-v2 boto3). 프로파일에 따라 주입
- 발신 주소 no-reply@yologram.link, 리전 ap-northeast-2. 자격증명: prod ECS Task Role, 로컬 AWS_PROFILE
- user_email_verification 테이블(email, code 6자리, verified, expiredAt 5분, createdAt). 코드 발송→검증(verified=true)→가입 시 확인→가입 후 삭제. 재발송 시 기존 삭제 후 생성

### 비밀번호 찾기
- 이메일 6자리 코드 발송→검증→재설정 (이메일 인증과 동일 패턴/SES 재사용)
- 별도 테이블 user_password_reset_code (회원가입 인증 로직과 분리해 회귀 위험 최소화, api-v1·v2 DB 공유)
- send(미가입 404 USER_NOT_FOUND, 기존 코드 삭제 후 발송) → verify(verified=true, 프론트 게이팅) → confirm(email·code·newPassword 재검증 후 변경·코드 삭제)
- 예외: UserPasswordResetExpiredException/UserPasswordResetInvalidException (400)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한 (현재 평문·무제한)

### 회원탈퇴 데이터 정리 전략 (soft delete 전환 시 결정)
- 현재(개발 단계): 레코드 즉시 하드 삭제(UserService.withdraw) → email 해제로 재가입 가능
- 탈퇴 요청과 연관 데이터(게시글/댓글/좋아요) 삭제를 분리해 요청 부하 완화
- 1차(동기, 즉시 응답): status=DELETED + deletedDate 기록, 토큰 무효화 후 204. 조회 시 DELETED 필터링 → 즉시 탈퇴 효과
- 2차(비동기 연관 삭제) 옵션:
  - SQS 이벤트 + 워커 (권장, 확장성): "user-deleted" 발행 → 워커가 도메인별 배치 삭제, 실패 시 DLQ 재시도
  - 배치 잡 (간단): DELETED 유저를 주기 스캔해 청크 삭제, 별도 인프라 최소
  - 앱 내 @Async/BackgroundTasks (소규모/임시): 요청과 분리하나 인스턴스 종속 → 배포/장애 시 유실 위험, 대량 부적합
- 대량 삭제는 청크(LIMIT N) 단위 반복으로 락/replica 지연 완화. 보관 의무 데이터는 익명화·유예기간(복구) 검토(soft delete면 자연 지원)
- 권장: soft delete 즉시 응답 + SQS 워커 도메인별 청크 삭제 (규모 작으면 배치 잡으로 시작)

### 커뮤니티 게시글 데이터 모델: 하이브리드 (단일 + section)
- post (단일 테이블 + section 컬럼): 공통 필드(id, section, user_id, title, content, like_count, comment_count, created_at, modified_date)
  - 섹션별 전용 필수 필드(투자 종목코드/수익률 등)는 1:1 확장 테이블로 분리(예: invest_post_detail), 해당 섹션 구현 시 추가. 엔드포인트 분리 없이 body 확장으로 처리
- 단일 테이블 채택 이유: 댓글/좋아요/저장/신고 등 상호작용이 섹션 무관 동일 → 자식 테이블·로직을 section마다 복제 안 함. 섹션 페이지·내 글·어드민은 모두 WHERE section=?로 처리. "전용 필드만" 갈리니 그 부분만 확장 테이블(하이브리드)
- 성능: 복합 인덱스 (section, id) = idx_post_section_id로 범위 스캔. 페이지네이션은 cursor(keyset), OFFSET 지양. 더 커지면 파티셔닝 검토
- post_category_mapping (N:M): post_id, category_id. pms 소유, 카테고리 필터 조회용. 경계 넘는 참조(user_id, category_id)는 FK 없이 인덱스. post/post_category_mapping은 api-v1·v2 DB 공유

### 커서 페이지네이션: id-only (legacy 방식과 일치)
- id가 auto_increment라 작성순=시간순 → id desc만으로 최신순. (section, id) 인덱스로 필터+정렬+커서를 한 번에 커버
- created_at 복합 커서는 정렬 기준이 id와 어긋날 때(인기순 등)나 정렬 키가 비유니크일 때만 필요 — 단순 최신순 피드엔 과함
- 종료 판정: +1/hasNext/count 없이 "마지막 글 id를 항상 nextCursor로, 빈 결과면 null". 클라이언트(TanStack useInfiniteQuery)는 nextCursor 유무로만 판단
- 구현: PostCursor(id-only Base64). N+1 회피 위해 닉네임(findNicknames, ums 경계 추상화 위해 join 대신 별도 조회)·카테고리(findByPostIds, 1:N이라 IN) 배치 조회, categoryId 필터는 EXISTS. api-v1은 PostRepositoryImpl(QueryDSL) + QuerydslConfig의 JPAQueryFactory 빈

### 카테고리: 동적 관리 (DB + 어드민 CRUD) — cms 소유
- 게시판마다 다른 분류 체계를 코드 상수가 아닌 DB로 관리. 카테고리 마스터는 cms 도메인(어드민 관리 메타데이터). 프론트는 GET /cms/{section}/categories로 섹션별 필터를 동적 조회
- post_category 테이블(id, section, name, sort_order, is_active, created_at, UNIQUE(section, name)). 갈리는 컬럼 없어 section별 분리 X. api-v1·v2 DB 공유
- PostCategoryService.getPostCategories(sectionPath): Section.fromPath 검증(대소문자 무시), is_active=true, sort_order 정렬. 응답 PostCategoryResponse { id, name, sortOrder }
- post_category_mapping(N:M)은 pms 소유, 글당 최대 3개 앱 검증. 작성 시 categoryIds 검증은 pms 책임 → 모놀리식은 post_category 직접 조회(LocalPostCategoryQueryClient), 분리 후 cms HTTP 호출 구현으로 교체 (카테고리는 거의 정적이라 캐시 가능)
- 어드민 카테고리 삭제 시 기존 글 처리(is_active 비활성 vs 매핑 제거)는 어드민 기능 구현 시 결정

### section은 ENUM (테이블화 보류)
- section은 페이지·라우트·전용필드와 코드에 강하게 결합 → row 추가만으로 동작하는 페이지가 안 생김(categories와 다른 점)
- sections 테이블 대신 Section enum(TECH/INVEST/POLITICS) + VARCHAR(20) 저장(UserType/UserStatus와 동일 패턴). 표시명/테마색 등 메타데이터 욕구 생기면 그때 테이블 도입 검토

### count / comment 경계
- like_count, comment_count는 현재 post 컬럼 동기 보관. /count 경로는 예약만, 추후 count 서비스로 이관
- 댓글은 comment 도메인 예정(/comments). community_comments.post_id는 FK 없이 인덱스 + app-level 검증(분리 대비)

### QueryDSL 사용 기준 (api-v1)
- 단순 단건·고정 조건(findBy/existsBy)·기본 CRUD → Spring Data JpaRepository로 충분
- 다음 중 하나라도 해당하면 QueryDSL custom repository: ①동적 조건 ②다중 조인(2개+ 엔티티) ③projection(필요 컬럼만 DTO) ④조건부 정렬/cursor·offset 페이지네이션 ⑤벌크 update·delete
- yologram 예: "내 글 목록"(section·작성자 필터 + 카테고리 조인 + 페이지네이션) = pms + QueryDSL / 공개 섹션 피드·키워드 검색 = search
- 구현 메모: PostRepositoryImpl.findPostsBySection이 첫 QueryDSL 사용처 — 동적 조건(categoryId/cursor) + EXISTS 서브쿼리 + keyset
- 함정: enum을 담는 패키지명에 `enum`(Java 예약어) 금지. QueryDSL APT가 `import ...enum.Xxx`를 생성 못 해 해당 enum 필드가 Q클래스에서 통째 누락됨(다른 타입 필드는 정상). cms.enum → cms.enums로 해결. ums.enum도 동일 잠재 이슈(현재 QueryDSL 미사용이라 보류)

### api-v2: Spring Boot(v1) ↔ FastAPI(v2) 대응 / 설정 관리 / 검증 응답 통일
- 대응 구조:

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

- 설정 관리: 로컬은 .env(pydantic-settings BaseSettings, APP_PROFILE 분기, 12-Factor). Secret은 AWS Parameter Store → ECS Task Definition secrets로 컨테이너 환경변수 주입, 코드는 os.environ 접근(프레임워크 의존 없음). Spring Boot는 프레임워크가 Parameter Store를 직접 통합하지만 FastAPI는 인프라(ECS/K8s)에 secret 주입을 맡기는 게 표준
- 결정: 패키지 매니저 uv, 설정 pydantic-settings + .env(YAML 아님), Secret은 ECS secrets, Python 3.12+
- 검증 응답 통일(api-v1 정합): RequestValidationError 핸들러가 status 422 → 400, errorCode VALIDATION_ERROR, 메시지는 첫 에러를 사람이 읽을 단일 문자열로(Pydantic "Value error, " 접두 제거). 라우팅 예외도 동일 형식: 404 → NOT_FOUND, 405 → METHOD_NOT_ALLOWED (StarletteHTTPException 핸들러). 작성 검증 메시지는 api-v1과 동일 문구("내용을 입력해주세요.", "카테고리는 1~3개 선택해주세요.")

### 검색 시스템 (search, 추후 도입) — 번개장터 구조 참고
- pms vs search 호출 기준: 단건 정확 조회·쓰기·"내 것"(개인화+권한) = pms / 공개 다건 탐색(키워드·카테고리·섹션 목록·필터·정렬·집계) = search
- CQRS: pms = 쓰기 원본(MySQL, 권한·개인화) / search = 읽기 최적화(OpenSearch). 동기화는 pms 쓰기 → 변경 이벤트(SQS/Kinesis) → indexer가 MySQL 읽어 문서화 → OpenSearch 인덱싱 (최종 일관성)
- QueryDSL vs search 역할: QueryDSL은 관계형 복잡성(권한 한정 "내 것/정확"), search는 탐색 복잡성(풀텍스트·연관도·패싯, 공개 카탈로그 발견)
- 도입 전략: 초기엔 pms cursor 목록으로 시작 → 검색·복잡 필터·대량 트래픽 필요 시 OpenSearch+indexer 도입(YAGNI). 별도 서비스 예정(yologram-search-api, yologram-search-indexer)

### 프론트 기술 스택 / React vs Next.js 비교 (학습 목적)
- web-v1: React 19, React Router 7, Vite, Yarn Berry(non-zero-install), Node 24, Ant Design + CSS Modules, Jotai, axios + TanStack Query
- web-v2: Next.js(App Router), TypeScript, Ant Design, Emotion, TanStack Query, Jotai, axios, Docker(Yarn Berry non-zero-install, 일반 next start)
- 비교 포인트: 라우팅 React Router(코드 기반) vs App Router(파일 기반) / 환경변수 VITE_ vs NEXT_PUBLIC_ / 빌드 Vite 정적 파일 vs Next.js node 서버 / 배포 S3+CloudFront vs node server / SSR React는 CSR only, Next.js는 SSR/SSG 가능
- web-v1 디렉토리: 페이지별 디렉토리(pages/invest/ 등), CSS는 컴포넌트와 같은 디렉토리에 .module.css, 글로벌만 styles/. UI는 Ant Design 우선
- web-v2 디렉토리: app/(페이지) apis/ components/ hooks/ queries/ stores/ styles/ types/ utils/

### 프론트 인증 상태/게이팅
- JWT는 Jotai authAtom + localStorage 저장, AuthState에 name 필드 포함. web-v2는 atomWithStorage getOnInit: true로 새 탭/새로고침 시 null 방지
- AuthGate가 앱 시작 시 저장 토큰을 validate-token으로 검증 후 렌더링. RequireAuth는 인증 초기화 완료 후 보호 라우트 진입 여부만 판단
- 401 인터셉터: authAtom 초기화(토큰 만료/무효 시 자동 로그아웃), /ums/auth/ URL 제외(로그인 실패 시 auth 초기화 방지), redirect 제거(초기화만)
- 로그아웃: localStorage.removeItem + window.location.href (RequireAuth 타이밍 이슈 회피)
- Next.js 차이: AuthGate를 providers.tsx에서 래핑(web-v1은 BrowserRouter 밖), RequireAuth children prop 기반(web-v1은 Outlet 기반), window 접근은 'use client'에서만

### 프론트 유효성/UX
- 프론트 validation은 서버와 동일(이메일 형식, 이름/닉네임 2~20자, 비밀번호 8~20자)
- 이메일 인증 완료 전 회원가입 버튼 비활성(EMAIL_NOT_VERIFIED 사전 차단), 이메일 변경 시 인증 상태 초기화·재발송
- 비밀번호 찾기 폼 유효성 게이팅(코드 발송/인증/변경 버튼), 이메일 변경 시 단계 초기화
- 입력 폼 제출 버튼은 클라이언트 유효성 통과 시에만 활성화(Ant Form은 useFormSubmittable 훅, 수동 상태 폼은 파생 isValid). antd App 래퍼로 모달/메시지 테마 일관
- 피드 연동: cursor 무한스크롤은 백엔드 id-only 커서와 일치(nextCursor 유무로만 판단), PostCard는 PostSummary 기반·상대시간 표시

### 프론트 환경 분리 / 배포
- web-v1: .env.development/.staging/.production, Vite 모드 분리(--mode), VITE_ prefix, 로컬 포트 3001·API http://localhost:5001. multi-stage 빌드 → nginx 정적 서빙, S3 + CloudFront(index.html no-cache·나머지 1년 캐시, Vite 해시 파일명)
- web-v2: NEXT_PUBLIC_ prefix, 서버 런타임 env(APP_ENV)는 .env 아닌 실행 주체(package script, ECS)가 주입, public env(NEXT_PUBLIC_APP_ENV)는 .env 유지로 역할 분리, 환경명은 APP_ENV 우선·없으면 NEXT_PUBLIC_APP_ENV fallback. multi-stage 빌드(yarn install --immutable), 일반 next start(standalone 미사용), 포트 3000

### web-v2 Observability 설계
- 1차는 server-side trace만 적용(확장으로 metrics 추가). src/instrumentation.ts에서 register, Node runtime에서만 OpenTelemetry SDK 초기화(endpoint 없으면 setup 생략)
- Trace: App Router 요청/렌더링/fetch span 수집 → Grafana Cloud OTLP direct push. Metrics: NodeSDK 직접 구성 direct push, host-metrics(process.cpu.utilization, process.memory.usage)
- HTTP 메트릭: instrumentation-http 추가했으나 Next.js 환경에서 http.server.request.duration 미생성 확인(한계). 요청 수는 현재 trace 기반 확인
- production 장기 권장안은 Alloy지만 현재는 direct push
