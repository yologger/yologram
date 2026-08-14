# yologram-api-v1 프로젝트 지침

## 프로젝트 개요

Spring Boot MVC (Kotlin) API 서버. ECS Fargate에서 운영.

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- src/main/resources/application.yaml: 공통 설정 (OTLP endpoint, resource attributes)
- src/main/resources/logback-spring.xml: 로깅 설정 (콘솔 + OTEL appender)
- src/main/kotlin/.../config/observability/OpenTelemetryLoggingConfig.kt: OTEL logback 초기화
- src/main/kotlin/.../config/database/QuerydslConfig.kt: JPAQueryFactory 빈
- src/main/kotlin/.../domain/pms/tech/publisher/event/: 조회 이벤트 발행 — PostViewEvent(계약, worker와 문자열 미러) + PostViewEventPublisher(PutRecord, partitionKey=postId, 실패 삼킴). 스위치는 yologram.events.publish.post-view.{enabled,stream}(EventPublishProperties) — 기본 비활성이라 로컬·테스트는 발행 스킵. SDK 클라이언트 빈은 config/kinesis/KinesisConfig(리전 명시·타임아웃 1s)
- src/main/kotlin/.../domain/search/tech/: 검색 인덱싱 요청 — publisher/message(TechPostIndexMessage 계약, worker와 문자열 미러 + Publisher — SQS SendMessage, 큐 URL 캐시. 조회 이벤트와 달리 실패를 전파한다: 어드민이 명시 요청한 작업), service/AdminTechPostIndexingService(범위를 20건 청크로 쪼개 발행 — 가시성 타임아웃 초과·재시도 단위·병렬성 때문), resource(PUT /search/admin/tech/posts/indexing{,/{id},/{from}/{to}} — 전부 202 + 어드민 토큰. indexing은 조작 세그먼트 — /posts에 PUT을 걸면 게시글 수정으로 읽힌다). 스위치는 yologram.messages.publish.post-index.{enabled,queue}(MessagePublishProperties), SDK 빈은 config/sqs/SqsConfig. 전체 인덱싱만 @Async("sqsTaskExecutor")로 돌린다 — 발행 루프를 요청 스레드에서 돌리면 게시글이 늘수록 게이트웨이 타임아웃(30초)에 걸린다(로컬 실측 202/7.8ms). 실제 색인은 하지 않는다(OpenSearch 클라이언트 없음 — worker 담당)
- src/main/kotlin/.../domain/search/tech/{document,repository,service,resource}/: 게시글 검색 — GET /search/tech/posts?q&page&size&sort. multi_match(title^2·content, nori 형태소), offset 페이징(page→from), 정렬 RELEVANCE(_score→createdAt)/LATEST(createdAt→_score). alias(tech-post-index)만 참조하고 실제 인덱스명은 쓰지 않는다(무중단 재색인 전략). trackTotalHits(true) — 기본값이면 10000건에서 카운트가 고정돼 마지막 페이지가 틀어진다. 색인에 없는 닉네임·likedByMe는 응답 조립 시 배치 조회로 채운다(개인화·변경 빈도 때문에 색인 대상이 아니다 — 번장 pipeline/resultenhancer와 같은 결). 조건부 빈(opensearch.main.enabled)이라 테스트는 @TestPropertySource로 켠다
- src/main/kotlin/.../config/opensearch/: OpenSearch 클라이언트 (worker 미러) — transport에 Boot ObjectMapper 주입(기본 mapper는 Java 8 날짜를 모른다), destroyMethod는 transport에 건다(클라이언트엔 close()가 없다)
- src/main/kotlin/.../domain/search/exception/: search 도메인 예외·핸들러 — 범위 오류를 400으로(전역 폴백은 500이라 Swagger 문서와 어긋난다)
- src/main/kotlin/.../infra/client/{ums,cms,pms,comment}/: 도메인 간 경계 클라이언트 — {대상도메인}ApiClient + Local 구현 (타 도메인 리포지토리 import는 이 층에서만, MSA 시 Rest 구현으로 교체)
- src/main/kotlin/.../config/redis/RedisConfig.kt·CacheRedisProperties.kt + infra/cache/: Valkey 캐시 — cache.data.redis.* 커스텀 프로퍼티 + 수동 Lettuce 빈(자동구성 exclude, DataSource와 동일 패턴). Cache<V> 키 팩토리(prefix:v1:entity:id)·CacheService(runCatching 폴백, getAllAsMap 배치)·UserNicknameCache(cache-aside 공용, loader 주입)·TechNewsFirstPageCache(뉴스 첫 페이지 응답 통째, 키 news:tech:v1:first-page:{categoryId|all}:{size}·TTL 3분 — worker가 요약 시 키 전수 열거 UNLINK로 무효화)
- src/main/kotlin/.../domain/ums/service/AuthService.kt: JWT 로그인/로그아웃/토큰 검증. validate-token은 master DB 조회
- src/main/kotlin/.../domain/ums/service/UserService.kt: 회원가입(이메일 인증 확인)·정보 수정·비밀번호 변경·회원탈퇴
- src/main/kotlin/.../domain/ums/service/UserEmailVerificationService.kt + EmailSender(Stub/Ses)·SesConfig: 이메일 인증·발송
- src/main/kotlin/.../domain/ums/service/UserPasswordResetService.kt: 비밀번호 찾기
- src/main/kotlin/.../domain/ums/service/AdminUserService.kt: 어드민 생성(어드민 토큰 가드, 항상 role=ADMIN)·로그인·토큰 검증·로그아웃·목록(offset 페이지)·삭제(자기 자신·OWNER 금지)·상태 변경(OWNER 전용, INACTIVE는 로그인·검증 403) — admin_user 테이블(role: OWNER는 DB 직접 조작 전용), 전용 JWT
- src/main/kotlin/.../domain/{pms,cms,comment,news}/tech: 도메인 우선 구조 — pms/tech(TechPostService·QueryDSL), cms/tech(TechCategoryService — tech_category 공용 마스터), comment/tech(TechPostCommentService), news/tech(TechNewsService — 공개 조회: 복합 커서·categoryId 필터·라벨 조인. AdminTechNewsSourceService — 어드민 소스 CRUD /news/admin/tech/sources)

## 설정 관리

- config/는 관심사별 하위 패키지로 나눈다 — database(DataSource·JPA·QueryDSL)·redis·kinesis·sqs(SqsClient·MessagePublishProperties)·ses·security·web·observability·async(AsyncConfig). 루트에 평면으로 두지 않는다(설정이 늘면 무엇이 무엇의 짝인지 안 보인다)
- application.yaml: 공통 설정 (OTLP endpoint placeholder)
- application-local.yaml: 로컬 개발 (AWS Parameter Store)
- application-prod.yaml: 프로덕션 (AWS Parameter Store, instance-profile)
- 설정값은 AWS Parameter Store에서 주입 (/yologram/service/yologram-api-v1_{ENV}/). local은 [prod, local] 순 import — 나중 선언이 우선이라 local 경로 값(cache.data.redis.host=localhost 등)이 prod를 덮음
- hbm2ddl: local=update, prod=validate — prod 테이블 생성·변경은 수동 DDL (정책·ENUM 함정은 docs/rules.md)

## 인증 (코딩 규칙)

- JWT: Auth0 java-jwt (HMAC256), 인증 헤더 Authorization: Bearer {token}
- 설정: yologram.auth.jwt.secret/expire/issuer/audience (Parameter Store + application.yaml)
- @AuthenticatedUser + AuthenticatedUserResolver로 인증 정보 주입
- 어드민: admin_user 분리 테이블 + 전용 JWT(yologram.auth.admin-jwt.*, audience yologram.admin). @AuthenticatedAdminUser + AuthenticatedAdminUserResolver 주입. 유저↔어드민 토큰 상호 불인정
- 인증 예외(AuthToken*)는 GlobalExceptionHandler에서 전역 처리 (ums 외 도메인 컨트롤러에서도 401 보장)
- access token은 stateless JWT (서버 미저장). validate-token은 로그인 직후 replica lag 회피 위해 master DB 트랜잭션으로 조회
- (동작·정책·refresh token 계획은 docs/done.md, docs/todos.md 참조)

## 이메일 인증 / SES (코딩 규칙)

- EmailSender 인터페이스로 발송 추상화: @Profile("!prod") StubEmailSender(로그), @Profile("prod") SesEmailSender(SesConfig에서 SesClient 빈 수동 등록)
- 발신 주소: no-reply@yologram.link (IAM 정책으로 한정, 변경 시 인프라 수정 필요)
- 리전: ap-northeast-2 (SES 도메인 인증 리전과 동일)
- 자격증명: ECS Task Role (prod), AWS_PROFILE 환경변수 (로컬)
- 비밀번호 찾기도 동일 패턴/SES 재사용 (UserPasswordResetService)

## 커뮤니티 (tech 게시판 코딩 규칙·함정)

- 섹션별 완전 분리: domain/{pms,cms,comment}/tech (도메인 우선, 섹션은 하위) — 테이블 tech_post/tech_post_category_mapping/tech_post_comment + tech_category(게시판·뉴스 공용 마스터) + tech_news/tech_news_category_mapping(뉴스 조회 전용) (전 테이블 무FK, section 컬럼·Section enum 없음 — 테이블명·경로·패키지가 섹션 담당). invest/politics는 동일 세트 복제로 추가
- 경계 검증·조회는 infra/client/{대상도메인}의 ApiClient로 추상화 (UmsApiClient·CmsApiClient·PmsApiClient·CommentApiClient + Local 구현 — 도메인 패키지 안에 두지 않음)
- TechPostRepositoryImpl이 QueryDSL 사용처. N+1 회피 위해 닉네임(findNicknames)·카테고리(findByPostIds) 배치 조회
- 댓글 수: tech_post_comment_count(pms 소유 1:1, post_id PK) — 갱신은 원자 upsert/가드 UPDATE만(엔티티 ±1 save 금지), 댓글 도메인은 PmsApiClient.increase/decreasePostCommentCount 경유
- 좋아요: tech_post_like 이력(UNIQUE(post_id,uid), 진실) + tech_post_like_count(1:1) — 이력은 INSERT IGNORE 한 문장(멱등, save+flush 예외 catch는 세션 오염이라 금지), 카운트 증감은 이력 변경 행수(1/0)로만 분기. POST/DELETE /pms/tech/posts/{id}/like 멱등 no-op 200
- 카운트 조회: 목록·상세 leftJoin+coalesce(0) 삼중 조인(TechPostWithCounts 프로젝션 — 댓글 수·좋아요 수·조회 수). 응답은 metrics: {commentCount, likeCount, viewCount, likedByMe} 중첩. 조회 수는 TechPostViewCount 읽기 전용 엔티티(쓰기는 worker 전담 — 증감 리포지토리 없음) — likedByMe는 선택 인증(@OptionalAuthenticatedUser — 헤더 없으면 null, 무효 토큰 401) + 이력 exists/IN 배치. tech_post의 comment_count·like_count 컬럼은 사장(매핑 제거 — drop 예정)
- 닉네임은 Valkey 캐시(ums:users:v1:nickname:{uid}, TTL 1h) 경유 — 히트 시 user 테이블 미접근, 무효화는 닉네임 변경·탈퇴 시 DEL. Redis 장애 시 DB 폴백(runCatching)
- 카테고리 매핑 교체는 @Modifying 벌크 delete 후 재삽입 (derived delete는 flush 순서로 uk 충돌)
- 응답 DTO의 section 필드는 "TECH" 고정 문자열 (web 계약 유지)
- 패키지명에 `enum`(Java 예약어) 금지 — QueryDSL APT가 import 생성 못 함. enums 사용
- (데이터 모델·엔드포인트·설계 근거는 docs/done.md, QueryDSL 사용 기준·경로 규칙은 docs/rules.md 참조)

## 비동기 (@Async)

- 스레드 풀은 config/async/AsyncConfig에 용도별로 정의하고 @Async("풀이름")으로 명시 호출한다 — 공유하면 오래 걸리는 작업이 스레드를 다 잡았을 때 다른 작업이 큐에서 대기한다
- 기본 풀(applicationTaskExecutor)도 직접 정의한다. Boot 자동구성은 @ConditionalOnMissingBean(Executor)라 Executor 빈을 하나라도 만들면 통째로 백오프하고, 그러면 이름 없는 @Async·MVC 비동기 요청 처리·JPA 부트스트랩이 SimpleAsyncTaskExecutor(호출마다 새 스레드)로 조용히 폴백한다. Executor 빈이 둘 이상이면 AsyncConfigurer.getAsyncExecutor()로 기본을 지정해야 한다
- 전용 풀은 @Bean(defaultCandidate = false)로 등록 — 타입 기반 자동 주입 후보에서 빼 기본 풀과 섞이지 않게 한다
- @Async에 올리는 작업은 결과를 아무도 기다리지 않는 것만. 예외가 호출자에게 전달되지 않으므로 메서드 안에서 runCatching으로 직접 잡아 남긴다. 큐가 인메모리라 인스턴스가 죽으면 대기분이 사라진다 — 유실되면 곤란한 작업은 SQS로

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- Testcontainers (MySQL) 통합 테스트
- Mockito + MockMvc 슬라이스 테스트
- @WebMvcTest는 WebConfig(WebMvcConfigurer)를 로드하므로 AuthenticatedUserResolver·AuthenticatedAdminUserResolver @Import + JwtUtil/JwtProperties·AdminJwtUtil/AdminJwtProperties @MockitoBean 필요

## Swagger

- Swagger UI: /api/v1/docs
- api-docs: /api/v1/api-docs
- 신규 API 추가 시 Swagger 문서화 필수

## CORS

- WebConfig에서 전체 허용 (*) — 인증 구현 시 origin 제한 필요

## 배포

- Docker (amazoncorretto:17-alpine, jar copy only)
- ECS Fargate
- GitHub Actions: Gradle build (--build-cache) → ECR push → ECS 재배포
