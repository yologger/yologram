# 기능 구현 시 따라야 할 내용

### API 경로 규칙
- /api/{v1|v2}/ums/ (유저), /pms/{section}/ (게시글), /cms/{section}/categories (카테고리)
- /comments/{section}/ (댓글 — 테이블 분리로 섹션 필수. 구경로 /comments/는 deprecated 위임, web 전환 후 제거), /count/ (카운트, 예약)
- 코드 패키지는 도메인 우선: domain/{도메인}/{섹션} (예: pms/tech, cms/tech, comment/tech, news/tech — MSA 분리 시 도메인 패키지째 이관, API 경로 /pms/{section}과 정합). 섹션(tech/invest/politics)은 경로 세그먼트이자 테이블 접두사이자 패키지의 섹션 세그먼트. 섹션별 코드는 완전 분리(공통 베이스 금지), 신규 섹션 = 테이블·코드 세트 복제(예: domain/pms/invest + InvestPostResource + /pms/invest/posts)
- 어드민: 도메인 경로 뒤 admin 세그먼트 → /{domain}/admin/... (게이트웨이 라우팅과 일관). 톱레벨 domain/admin 금지 — 어드민은 역할이지 도메인이 아니며, 전 도메인 repository를 관통해 경계 규칙 위반. 코드는 별도 admin 하위 패키지 없이 각 도메인 평면 구조에 클래스명 Admin 접두사(AdminUserService 등)로 구분

### 클라이언트 IP

- 원 클라이언트 IP는 X-Client-Ip 헤더에서 읽는다(v1 ClientIpResolver / v2 resolve_client_ip) — API Gateway HTTP API + private integration(VPC Link)은 백엔드 remoteAddr이 게이트웨이 ENI 주소이고, X-Forwarded-For는 파라미터 매핑 예약 헤더라 채울 수 없다. 원 IP 경로는 $context.identity.sourceIp뿐이라 통합에서 `overwrite:header.X-Client-Ip`로 주입한다(yologram-infra api-v1·v2 integration)
- overwrite여야 한다 — append면 클라이언트가 보낸 위조값이 앞에 남아 첫 값을 읽는 로직이 속는다. IP를 카운트 dedup 키로 쓰는 한 이건 조회수 조작 경로다
- 해석 순서는 X-Client-Ip → X-Forwarded-For 첫 값 → remoteAddr — XFF 폴백은 CloudFront·ALB 경로용으로 남긴다

### DB DDL 정책
- hbm2ddl: local=update(개발 편의 자동 반영), prod=validate(검증만) — prod 테이블 생성·변경은 사용자가 수동 DDL로 직접 수행 (api-v1·worker 공통)
- 신규 엔티티 추가 시: Testcontainers 테스트(create-drop) 로그에서 Hibernate 생성 DDL을 확인해 그대로 prod에 수동 실행 후 배포 (테스트 리포트 XML system-out에 create 문 포함)
- 함정: Hibernate 6.2+는 @Enumerated(STRING)을 MySQL 네이티브 ENUM 컬럼으로 생성(@Column length 무시) — 수동 DDL을 varchar로 만들면 validate 불일치 위험. Hibernate 생성문 기준으로 작성

### QueryDSL 사용 기준 (api-v1)
- 단순 단건·고정 조건(findBy/existsBy)·기본 CRUD → Spring Data JpaRepository로 충분
- 다음 중 하나라도 해당하면 QueryDSL custom repository: ①동적 조건 ②다중 조인(2개+ 엔티티) ③projection(필요 컬럼만 DTO) ④조건부 정렬/cursor·offset 페이지네이션 ⑤벌크 update·delete
- yologram 예: "내 글 목록"(section·작성자 필터 + 카테고리 조인 + 페이지네이션) = pms + QueryDSL / 공개 섹션 피드·키워드 검색 = search
- 구현 메모: PostRepositoryImpl.findPostsBySection이 첫 QueryDSL 사용처 — 동적 조건(categoryId/cursor) + EXISTS 서브쿼리 + keyset
- 함정: enum을 담는 패키지명에 `enum`(Java 예약어) 금지. QueryDSL APT가 `import ...enum.Xxx`를 생성 못 해 해당 enum 필드가 Q클래스에서 통째 누락됨(다른 타입 필드는 정상). cms.enum → cms.enums로 해결. ums.enum도 동일 잠재 이슈(현재 QueryDSL 미사용이라 보류)
- 함정: 게시글 수정 시 카테고리 매핑 교체(전체 삭제 후 재삽입)는 JPA(api-v1)에서 derived `deleteByPostId`를 쓰면 flush 순서상 insert가 delete보다 먼저 나가 uk_post_category(post_id, category_id) 충돌(1062). `@Modifying` 벌크 delete로 즉시 삭제 후 재삽입할 것. SQLAlchemy(api-v2)는 `.delete()`가 즉시 실행이라 무관. delete+insert는 같은 트랜잭션이라 중간 상태 노출·부분 실패 없음

### MSA 
-  지금은 모놀리틱이나 추후 MSA로 전환 예정이라, 가능하면 아래 내용을 참고하여 MSA 전환이 쉬운 구조로 구현
-  현재 경계 호출은 인터페이스(ApiClient/Protocol)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체. 모놀리식은 Local*ApiClient(리포지토리 직접) 구현, self HTTP 호출 지양
-  ApiClient: 도메인 간 호출은 infra/client/{대상도메인}의 {대상도메인}ApiClient로만 (UmsApiClient·CmsApiClient·PmsApiClient·CommentApiClient — 번장 bun-order-api infra/{대상}/client 패턴 미러). 구현은 Local 접두(모놀리식, 타 도메인 리포지토리 import는 이 층에서만 허용) → MSA 분리 시 같은 패키지에 Rest 구현+Config+dto 추가로 교체. 클라이언트당 인터페이스·구현 각 1개(소비 도메인별 중복 금지)
-  단일 서버에서 도메인별 패키지로 분리. 도메인 경계 = 미래 MSA 경계 = API Gateway path prefix
  -  UMS→yologram-user-api, PMS→yologram-post-api, CMS→yologram-cms-api, Comment→comment-api, Count→count-api, News→news-api
-  분리 전략: 도메인 간 직접 JOIN·repository 교차 호출 금지
-  전 테이블 FK 미사용(같은 도메인 내부 포함 — tech_news에서 같은 도메인 FK 허용했다가 TRUNCATE 불가 등 운영 불편으로 제거). 참조는 컬럼+인덱스 + app-level 검증
-  경계 넘는 동기 트랜잭션 의존 최소화 (count 갱신은 추후 이벤트/최종일관성)

### 카운트 (댓글 수·좋아요 등)

- 카운트 갱신은 원자 쿼리로만 — INSERT...ON DUPLICATE KEY UPDATE(+1) / 가드 UPDATE(count>0, -1). "엔티티 읽고 ±1 후 save"는 lost update 레이스라 금지 (레거시 방식 답습 금지)
- 1:1 카운트 테이블(post_id PK)은 대상 도메인 소유(pms) — 타 도메인(comment 등)의 갱신은 ApiClient 경유. count 0 row는 삭제하지 않음(조회 coalesce가 0 처리)
- 목록·상세 조회는 leftJoin+ON 명시(무FK)+coalesce(0) — 1:1이라 row 뻥튀기 없음. 고빈도 쓰기 카운트(조회수)는 동기 갱신 대신 이벤트 스트리밍(Kinesis→worker 적재, done.md)
- 토글류(좋아요 등 "누가"가 필요한 카운트)는 이력(UNIQUE(대상,uid))+카운트 분리 — 이력이 진실, API는 멱등(중복/무상태 호출 no-op 200). 이력 삽입은 INSERT IGNORE(v2는 insert().prefix_with("IGNORE")) 한 문장 — save 후 uk 예외 catch는 Hibernate 세션 오염이라 금지. 카운트 증감은 이력 변경 행수(1/0)로만 분기
- 쓰기 주체가 worker인 카운트(조회수)는 API에 읽기 전용 엔티티만 둔다 — 증감 리포지토리를 만들지 않는다(동기 갱신 경로가 생기면 이벤트 파이프라인과 이중 소스가 된다). 갱신은 이력 적재와 같은 트랜잭션에서 worker가 수행
- 게시글 응답의 카운트는 metrics 객체로 중첩(metrics: {commentCount, likeCount, viewCount, likedByMe}) — 새 카운트(viewCount 등)는 metrics에 필드 추가(무브레이킹). 평면 카운트 필드 신설 금지. tech_post의 like_count·comment_count 컬럼은 사장(매핑 제거됨 — drop 예정, 참조 금지)
- 개인화 값(likedByMe)은 카운트 프로젝션에 넣지 않고 service에서 이력 조회(상세 exists·목록 IN 배치). 공개 GET에서 개인화가 필요하면 선택 인증(v1 @OptionalAuthenticatedUser / v2 get_optional_authenticated_user) — 헤더 없으면 비로그인, 있으면 검증(무효 401)

### 이벤트 스트리밍 (Kinesis)

- 조회 이벤트는 섹션별로 스트림을 나누지 않는다 — 스트림 하나 + 페이로드 section 필드로 분기(과금 단위가 샤드라 스트림 분리는 비용 배수). 스트림을 나눌 근거는 샤드 한계 초과·보관/보안 경계 상이·head-of-line 차단뿐
- 설정 경로는 발행·구독 대칭 — 발행 yologram.events.publish.{이벤트}.{enabled,stream} / 구독 yologram.events.subscribe.{이벤트}.enabled. 둘 다 enabled 기본값 false(로컬·테스트가 prod 스트림·체크포인트를 건드리지 않게)이고 prod 프로파일에서만 켠다. api-v2는 yaml이 없어 pydantic-settings 평면 대문자 매핑으로 대응(POST_VIEW_PUBLISH_ENABLED/POST_VIEW_PUBLISH_STREAM)
- enabled=true인데 대상(stream)이 비면 warn 로그를 남긴다 — 조용히 스킵하면 발행이 0건인 이유를 알 수 없다
- 발행·구독 코드는 domain/{도메인}/{섹션}/{publisher|subscriber}/{수단} — 수단은 event(스트림·Kinesis) / message(SQS). 의미(도메인 이벤트 vs 명령)가 아니라 전송 수단 기준이다. 이벤트 계약 클래스도 같은 자리에 둔다(api-v1 publisher/event/PostViewEvent, worker subscriber/event/PostViewEvent — 문자열 계약이라 한쪽 변경 시 양쪽 동시 수정). SDK 클라이언트 빈·프로퍼티 클래스는 config/에 남긴다(SesConfig·RedisConfig와 같은 층)
- 발행은 실패를 삼킨다(사용자 응답이 스트림 장애로 깨지지 않게) — 대신 소비 쪽이 at-least-once를 전제로 멱등해야 한다. 멱등 키는 발생 시각(occurredAt) 기준으로 만들 것(처리 시각 기준이면 재처리 때 키가 달라져 멱등이 깨진다)
- 수동 체크포인트는 반드시 DB 커밋 이후 — 먼저 찍으면 유실, 나중이면 중복이고 중복은 uk가 흡수한다
- 포이즌 레코드(깨진 JSON·미지원 eventType/section)는 예외를 올리지 않고 스킵+warn — 예외를 올리면 그 배치가 체크포인트되지 못해 영구 재처리로 소비가 멈춘다
- binder는 KCL 모드(kpl-kcl-enabled=true)로 쓴다 — 리샤딩·워커 증설 대응. EFO(fan-out)와 CloudWatch 메트릭은 끈다(과금 회피, 지표는 OTLP). DynamoDB 리스 테이블은 KCL이 자동 생성하므로 tf로 만들지 않고 IAM에 CreateTable만 부여 — 이 테이블을 지우면 체크포인트가 사라져 그 사이 이벤트가 유실된다
- KCL 컨슈머를 얹는 태스크는 CPU 여유가 필요하다 — 0.25vCPU에서 Netty 이벤트 루프가 굶어 SDK 커넥션 획득 타임아웃으로 소비가 멈춘 선례(done.md 사고 ③). 배치·LLM 호출과 코어를 공유하면 0.5vCPU 이상
- AWS SDK를 쓰는 서비스는 리전을 명시한다 — 클라이언트마다 .region() 또는 spring.cloud.aws.region.static. 자동 탐색 체인은 CI·ECS Fargate에서 채워지지 않아 기동이 실패한다

### 타임존

- 모든 서비스는 기본 타임존을 Asia/Seoul로 고정한다 — JVM은 진입점에서 TimeZone.setDefault, Python은 컨테이너 ENV TZ. 하나라도 빠지면 그 서비스만 UTC 벽시계로 시각을 만들어 같은 레코드가 서비스마다 다르게 보인다(api-v1 누락으로 createdAt이 9시간 어긋난 선례)
- UTC 저장 후 표시 시점 변환이 아니라 KST 통일이다(번장 전 서비스 방식) — 국내 서비스라 UTC를 경유할 이유가 없고, 변환 지점이 없으면 변환 실수도 없다
- hibernate.jdbc.time_zone·JDBC serverTimezone만 맞춰도 되는 문제가 아니다. LocalDateTime.now()와 Timestamp.toLocalDateTime()이 JVM 기본 TZ를 쓰기 때문에 JVM TZ 자체를 고정해야 한다
- 새 서비스·새 컨테이너를 추가할 때 이 설정을 함께 넣을 것

### 검색 인덱싱 (SQS + OpenSearch)

- 인덱싱 큐는 대상별로 나누지 않는다 — 큐 하나(yologram-search-indexing-{env}) + 페이로드 target 필드로 분기. tech-post·politics-post·뉴스가 늘어도 큐는 그대로다(소비 순서·처리 특성이 같고, 큐가 늘면 워커 리스너·IAM·DLQ가 배수로 는다). 큐를 나눌 근거는 처리량 격차로 인한 head-of-line 차단뿐
- 단건 인덱싱도 범위 메시지로 보낸다(from == to) — 경로가 하나면 소비·재시도·테스트가 한 벌로 끝난다. 레거시는 단건만 동기 처리(200)했지만 우리는 전부 202 비동기
- 범위는 20건 청크로 쪼개 발행한다 — ① 한 메시지 처리가 가시성 타임아웃(300초)을 넘기면 재노출돼 중복 처리 ② 실패 재시도 단위가 작아짐 ③ 워커를 늘리면 메시지 단위로 병렬화. 청크 크기는 레거시 BULK_INDEXING_REQUEST_BATCH_SIZE 미러
- 설정 경로는 발행·구독 대칭 — 발행 yologram.messages.publish.{작업}.{enabled,queue} / 구독 yologram.messages.subscribe.{작업}.{enabled,queue}. events.*(Kinesis)와 같은 규칙이고 기본값 false, prod만 true
- 인덱싱 발행은 실패를 삼키지 않는다 — 조회 이벤트(사용자 응답을 막지 않아야 함)와 반대로 어드민이 명시 요청한 작업이라 실패를 알려야 한다
- SQS 소비는 수동 ack(MANUAL) — 색인이 끝난 뒤에만 삭제한다. 먼저 지우면 실패한 범위가 영영 색인되지 않는다. 실패 시 ack하지 않아 가시성 타임아웃 후 재전달되고 3회 실패면 DLQ로 간다
- 다시 시도해도 결과가 같은 메시지(깨진 JSON·미지원 target·유효하지 않은 범위)는 ack해서 버린다 — DLQ 왕복은 낭비다. 반대로 일시적 실패(OpenSearch 다운·bulk 거부)는 반드시 ack하지 않는다
- 범위 필드(from·to)는 원시 타입이라 필드가 빠져도 파싱이 통과하고 0이 들어온다 — 소비 쪽에서 범위를 검증할 것(검증 없으면 잘못된 메시지가 index(0,0)으로 조용히 성공한다)
- 메시지 계약 클래스는 미지의 필드를 무시한다(@JsonIgnoreProperties) — 발행 쪽을 먼저 배포해도 소비가 깨지지 않게. api-v1·worker 양쪽에 같은 클래스를 두는 문자열 계약이라 필드명 변경은 양쪽 동시 수정
- 문서 id는 원본 id를 그대로 쓴다 — at-least-once 재전달·중복 발행이 덮어쓰기가 되어 무해해진다(멱등)
- 인덱스는 버전 인덱스 + alias로 운용한다: tech-post-index-v1(실제) / tech-post-index(alias). 애플리케이션은 alias만 참조하고, 매핑을 바꿔야 하면 v2를 만들어 재색인한 뒤 alias를 옮긴다(무중단). 인덱스·alias는 워커 기동 시 upsert — 없는 상태로 색인하면 동적 매핑이 걸려 title/content가 nori 없이 잡힌다
- 한국어 분석은 nori(yologram_korean, decompound_mode=mixed) — 공식 OpenSearch 이미지에 미포함이라 컨테이너 기동 시 플러그인을 설치한다. 단일 노드라 number_of_replicas=0(기본 1이면 인덱스가 영구 yellow)
- 인덱싱 API는 어드민 토큰을 요구한다 — 공개로 열면 누구나 풀 인덱싱을 유발해 OpenSearch·DB에 부하를 준다
- 인덱싱 경로는 대상 뒤에 조작 세그먼트를 붙인다(/search/admin/tech/posts/indexing) — /posts에 PUT을 걸면 게시글 수정으로 읽히고 검색 API(/search/tech/posts)와 성격이 뒤섞인다. 인덱스 관리(/posts/indices)·재색인 전환(/posts/migration)이 형제로 붙을 자리를 남겨둔다
- 발행 루프는 요청 스레드에서 돌리지 않는다 — 전체 인덱싱은 @Async로 넘기고 즉시 202. 청크가 수천 건이면 SendMessage 왕복만으로 게이트웨이 타임아웃(30초)을 넘는다. 진행 상황은 응답이 아니라 SQS 큐 깊이로 확인한다
- OpenSearch 접속은 opensearch.main.{uri,username,password}(SSM, SecureString) + enabled(yaml). 스위치는 환경별 고정값이라 파라미터로 빼지 않고, 파라미터 이름은 서비스가 달라도 동일하게 둔다(같은 클러스터를 가리키는 값이 서비스마다 다른 키를 갖지 않게)
- 색인 대상 데이터는 infra/client/{원본도메인}에서 읽는다(worker infra/client/pms) — 검색 도메인이 원본 테이블을 직접 매핑하지 않는다

### 검색 조회

- 개인화 값과 자주 바뀌는 값은 색인에 넣지 않는다 — likedByMe는 보는 사람마다 달라 문서에 담을 수 없고, 닉네임은 한 번 바뀌면 그 사람 글 전부를 재색인해야 한다. 검색 후 배치 조회로 채운다(번장 pipeline/resultenhancer와 같은 결)
- 왕복은 결과 건수와 무관하게 고정이다 — OpenSearch 1회 + 좋아요 이력 1회(로그인 시) + 닉네임 1회(캐시 미스분만). size를 올려도 IN 절 항목만 늘어난다
- 페이징은 offset(page→from) — 검색은 총건수·페이지 번호가 필요하고 커서로는 만들 수 없다. max_result_window(10000) 초과는 400으로 끊는다(방치하면 엔진 예외가 500이 된다)
- 정렬에는 2차 키를 둔다 — 1차 키 동점 시 순서가 흔들리면 페이징에서 같은 문서가 두 페이지에 나오거나 빠진다
- trackTotalHits를 켠다 — 기본값이면 10000건에서 카운트가 고정돼 마지막 페이지가 틀어진다
- 텍스트 필드는 nori(형태소) + standard(단어 통째) 두 분석기로 색인하고 검색은 둘 다 본다 — nori는 사전에 없는 외래어를 문맥마다 다르게 쪼갠다(실측: "마이그레이션"이 그레|이 / 마이|그레|이 / 마|이|그레이|션). 조사·어미 처리는 nori가, 외래어·고유명사는 standard가 담당한다
- multi_match는 operator를 AND로 둔다 — 기본값(OR)이면 nori가 만든 한 글자 토큰("이") 하나에 무관한 글이 대량 매칭된다(실측: "마이그레이션" 56건 → 2건). 조사 포함 검색("검색 기능을")은 AND에서도 그대로 동작한다
- 검색 비활성(opensearch.main.enabled=false) 환경에서는 503 SEARCH_UNAVAILABLE로 응답한다 — 조건부 빈·조건부 라우터로 경로를 없애면 404가 되어 "없는 경로"로 오해된다. api-v1은 @Lazy 빈 + ObjectProvider로 늦게 만들고 서비스가 판정, api-v2는 클라이언트가 lru_cache로 lazy라 라우터를 항상 등록한다
- 애플리케이션은 alias만 참조한다(tech-post-index) — 실제 인덱스명(-v1)을 쓰면 v2 재색인 후 alias 이동으로 무중단 전환하는 전략이 깨진다

### 비동기 (@Async)

- 스레드 풀은 config/async/AsyncConfig에 용도별로 정의하고 @Async("풀이름")으로 명시 호출한다 — 하나를 공유하면 오래 걸리는 작업이 스레드를 다 잡았을 때 다른 작업이 큐에서 대기한다
- 기본 풀(applicationTaskExecutor)도 직접 정의한다. Boot 자동구성은 @ConditionalOnMissingBean(Executor)라 Executor 빈을 하나라도 만들면 통째로 백오프하고, 그러면 이름 없는 @Async·Spring MVC 비동기 요청 처리·JPA 부트스트랩이 SimpleAsyncTaskExecutor(호출마다 새 스레드)로 조용히 폴백한다
- Executor 빈이 둘 이상이면 Spring이 @Async 기본을 정하지 못한다 — AsyncConfigurer.getAsyncExecutor()로 지정할 것
- 전용 풀은 @Bean(defaultCandidate = false)로 등록 — 타입 기반 자동 주입 후보에서 빼 기본 풀과 섞이지 않게 한다
- @Async에 올리는 작업은 결과를 아무도 기다리지 않는 것만. 예외가 호출자에게 전달되지 않으므로 메서드 안에서 runCatching으로 직접 잡아 남긴다(기본 핸들러에 맡기면 어디까지 처리됐는지 알 수 없다). 큐가 인메모리라 인스턴스가 죽으면 대기분이 사라진다 — 유실되면 곤란한 작업은 SQS로
- api-v2는 FastAPI BackgroundTasks가 대응한다 — 예외가 호출자에게 가지 않는 점이 같으므로 동기 메서드는 실패를 전파하고 백그라운드 진입점만 삼킨다
- SQS 리스너 안에서는 쓰지 않는다 — 예외가 리스너 밖에서 발생해 수동 ack·가시성 제어가 무력화된다(레거시 BoardIndexingHandler의 결함). 리스너는 이미 별도 스레드 풀에서 돈다

### 구조 (config 패키지)

- config/는 관심사별 하위 패키지로 나눈다 — database·redis·kinesis·sqs·ses·security·web·observability·async·opensearch·scheduling. 설정 클래스와 그 프로퍼티 클래스는 같은 패키지에 두어 각 디렉토리가 자기 완결적이게 한다(루트에 평면으로 쌓이면 무엇이 무엇의 짝인지 보이지 않는다)
- 패키지 이동을 스크립트로 할 때 주석·KDoc의 클래스명을 코드 참조로 오인하지 말 것 — unused import가 붙는다(컴파일은 통과해서 눈에 띄지 않는다)

### 캐시 (Valkey)

- 키 스킴 {도메인 prefix}:v1:{엔티티}:{식별자} — 정의는 각 API infra/cache의 Cache 팩토리(v1 Cache.kt / v2 cache.py). v1·v2가 같은 키·JSON(camelCase, ensure_ascii=False)을 공유하므로 한쪽 변경 시 반드시 양쪽 동시 수정
- 뉴스 첫 페이지 키(news:tech:v1:first-page:{categoryId|all}:{size})는 worker TechNewsFirstPageCacheInvalidator의 UNLINK 전수 열거와 문자열 계약 — 키 스킴·size 상한(50) 변경 시 api-v1·v2·worker 3곳 동시 수정
- 캐시에는 파생 데이터만(원본은 항상 MySQL) — 최악의 불일치는 flush로 복구 가능해야 한다. auth 상태 캐시 금지(INACTIVE 즉시 차단 무력화)
- 무효화 방식 기준: 키 공간이 열거 가능하면 DEL(UNLINK) 전수 열거 — SCAN 금지(전체 keyspace 순회라 무관한 키 증가에 비용 동반). 열거 불가능하면 버전 키(INCR) 검토
- 로컬 Redis는 localhost:16379 (brew redis — v1·worker application-local.yaml, v2 .env)

### Worker (yologram-worker)
- 워커 작업은 FARGATE_SPOT 중단(2분 경고 후 종료·재기동)을 전제로 멱등·재시도 가능하게 설계 (예: RSS 수집은 중복 방지 키, 삭제류는 청크 반복)
- @Scheduled는 놓친 사이클을 소급하지 않음 — 다음 주기가 커버하는 작업(RSS류)만 사용. 시각 민감/누적형/중단 불가 배치는 EventBridge Scheduler → SQS로 이관(스케줄 발화를 인프라가 보장)
- 주기 작업은 단일 인스턴스 전제 — 인스턴스 확장 시 ShedLock 도입
- 워커는 인바운드 없음(API GW·Cloud Map 미사용) — actuator는 ECS exec로 localhost:5000 접근
- LLM 무료 티어 모델 선정은 문서가 아닌 실측 기준 — 사용 가능 모델은 /v1/models 실조회, 쿼터는 429 응답의 quotaValue가 정답 (웹 문서는 낡음). 상위 모델(예: gemini-3.5-flash 20 req/일)은 무료 쿼터 극소량 — lite 계열 사용
- 워커 HTTP 호출은 WebClient 통일(global/client WebClientFactory — bun-client-kotlin 미러). 단 라이브러리가 스트림을 요구하면 바이트로 받아 넘김(RSS→Rome), Spring AI는 자체 RestClient에 타임아웃 주입(LlmConfig)
