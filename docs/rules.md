# 기능 구현 시 따라야 할 내용

### API 경로 규칙
- /api/{v1|v2}/ums/ (유저), /pms/{section}/ (게시글), /cms/{section}/categories (카테고리)
- /comments/{section}/ (댓글 — 테이블 분리로 섹션 필수. 구경로 /comments/는 deprecated 위임, web 전환 후 제거), /count/ (카운트, 예약)
- 섹션(tech/invest/politics)은 경로 세그먼트이자 테이블 접두사이자 코드 패키지(domain/{섹션}/{기능}) — 게시판·아티클 공통 규약. 섹션별 코드는 완전 분리(공통 베이스 금지), 신규 섹션 = 테이블·코드 세트 복제
- 어드민: 도메인 경로 뒤 admin 세그먼트 → /{domain}/admin/... (게이트웨이 라우팅과 일관). 톱레벨 domain/admin 금지 — 어드민은 역할이지 도메인이 아니며, 전 도메인 repository를 관통해 경계 규칙 위반. 코드는 별도 admin 하위 패키지 없이 각 도메인 평면 구조에 클래스명 Admin 접두사(AdminUserService 등)로 구분

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
-  현재 경계 호출은 인터페이스(QueryClient/Protocol)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: PostCategoryQueryClient, UserQueryClient). 모놀리식은 Local*QueryClient(리포지토리 직접) 구현, self HTTP 호출 지양
-  단일 서버에서 도메인별 패키지로 분리. 도메인 경계 = 미래 MSA 경계 = API Gateway path prefix
  -  UMS→yologram-user-api, PMS→yologram-post-api, CMS→yologram-cms-api, Comment→comment-api, Count→count-api, News→news-api
-  분리 전략: 도메인 간 직접 JOIN·repository 교차 호출 금지
-  경계 호출은 인터페이스(QueryClient/Protocol)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: PostCategoryQueryClient, UserQueryClient). 모놀리식은 Local*QueryClient(리포지토리 직접) 구현, self HTTP 호출 지양
-  전 테이블 FK 미사용(같은 도메인 내부 포함 — tech_article에서 같은 도메인 FK 허용했다가 TRUNCATE 불가 등 운영 불편으로 제거). 참조는 컬럼+인덱스 + app-level 검증
-  경계 넘는 동기 트랜잭션 의존 최소화 (count 갱신은 추후 이벤트/최종일관성)

### Worker (yologram-worker)
- 워커 작업은 FARGATE_SPOT 중단(2분 경고 후 종료·재기동)을 전제로 멱등·재시도 가능하게 설계 (예: RSS 수집은 중복 방지 키, 삭제류는 청크 반복)
- @Scheduled는 놓친 사이클을 소급하지 않음 — 다음 주기가 커버하는 작업(RSS류)만 사용. 시각 민감/누적형/중단 불가 배치는 EventBridge Scheduler → SQS로 이관(스케줄 발화를 인프라가 보장)
- 주기 작업은 단일 인스턴스 전제 — 인스턴스 확장 시 ShedLock 도입
- 워커는 인바운드 없음(API GW·Cloud Map 미사용) — actuator는 ECS exec로 localhost:5000 접근
- LLM 무료 티어 모델 선정은 문서가 아닌 실측 기준 — 사용 가능 모델은 /v1/models 실조회, 쿼터는 429 응답의 quotaValue가 정답 (웹 문서는 낡음). 상위 모델(예: gemini-3.5-flash 20 req/일)은 무료 쿼터 극소량 — lite 계열 사용
- 워커 HTTP 호출은 WebClient 통일(global/client WebClientFactory — bun-client-kotlin 미러). 단 라이브러리가 스트림을 요구하면 바이트로 받아 넘김(RSS→Rome), Spring AI는 자체 RestClient에 타임아웃 주입(LlmConfig)
