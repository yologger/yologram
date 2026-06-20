# yologram-api-v1 브레인스토밍

## 기술 스택

- Spring Boot 3.5 (Kotlin), Java 17
- Spring Data JPA (Hibernate) - 기본 CRUD
- QueryDSL - 복잡한 쿼리
- MySQL (RDS) - 프로덕션 DB
- Testcontainers (MySQL) - 로컬/CI 테스트
- BCryptPasswordEncoder - 비밀번호 해싱
- Auth0 java-jwt - JWT 토큰

## DB

- R/W splitting: MasterSlaveRoutingDataSource (레거시 패턴)
- @Transactional(readOnly = true) → Slave, 그 외 → Master
- 로컬: Testcontainers MySQL
- 프로덕션: RDS MySQL (Parameter Store에서 credential 주입)

## 도메인 구조

- 현재는 api-v1 단일 서버(모듈러 모놀리식)에서 도메인별 패키지로 분리해 처리
- 도메인 경계 = 미래 마이크로서비스 경계 = API Gateway path prefix. 나중에 prefix별로 다른 서비스로 포워딩해 물리 분리
  - UMS (User Management System): 회원/인증 — yologram-api-v1(현재) → yologram-user-api
  - PMS (Post Management System): 커뮤니티 게시글 + post_category_mapping(연결) → yologram-post-api
  - CMS (Contents Management System): 카테고리 마스터, (추후 기획전/배너/팝업 등 콘텐츠·메타데이터) → yologram-cms-api
  - Comment: 댓글 → yologram-comment-api
  - Count: 좋아요·댓글 수 등 카운트 → yologram-count-api
  - News: 뉴스 → yologram-news-api
- 분리 전략: 모듈러 모놀리식으로 시작, 도메인 간 직접 JOIN·repository 교차 호출 금지
  - 도메인 경계 호출은 인터페이스(클라이언트)로 추상화 → 분리 시 구현만 직접호출 → HTTP/gRPC로 교체
  - self HTTP 호출은 지양(오버헤드·트랜잭션 경계 상실). 모놀리식 단계는 직접 호출 구현
  - 분리 경계를 넘는 FK 지양(같은 도메인 내부만 FK). 경계 넘는 참조는 인덱스 + app-level 검증
  - 분리 경계를 넘는 동기 트랜잭션 의존 최소화(예: count 갱신은 추후 이벤트/최종일관성)

## API 경로 규칙

- /api/v1/ums/ - 유저 관리
- /api/v1/pms/{section}/ - 커뮤니티 게시글 (section: tech/invest/politics)
- /api/v1/cms/{section}/categories - 카테고리(contents)
- /api/v1/comments/ - 댓글 (추후)
- /api/v1/count/ - 좋아요·댓글 수 (경로 예약, 추후 분리)
- /api/v1/news/ - 뉴스 (추후)
- 어드민 API: 도메인 경로 뒤에 admin 세그먼트 추가 → /api/v1/{domain}/admin/...
  - 예: /api/v1/cms/admin/{section}/categories, /api/v1/pms/admin/posts, /api/v1/ums/admin/users
  - prefix(=서비스 경계) 유지하면서 어드민 구분. 게이트웨이 라우팅과 일관

## 유저 타입

- DEFAULT: 일반 사용자
- POLITICIAN: 정치인
- ECONOMIST: 경제인
- ADMIN: 관리자

## 인증 방식

- JWT (HMAC256)
- 토큰은 User 엔티티에 캐시 (accessToken 필드)
- 커스텀 헤더: Authorization Bearer 방식
- 로그아웃 시 accessToken null 처리

## 이메일 인증

- EmailSender 인터페이스로 발송 추상화 (테스트/개발: StubEmailSender, 프로덕션: SesEmailSender)
- email_verification_codes 테이블: email, code(6자리 숫자), verified, expiredAt
- 인증 흐름: 코드 발송 → 코드 검증 → verified=true → 회원가입 시 확인
- 같은 이메일로 재발송 시 기존 레코드 삭제 후 새로 생성
- 가입 완료 후 인증 레코드 삭제

## 비밀번호 찾기

- 방식: 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (회원가입 이메일 인증과 동일 패턴/SES 재사용)
- 저장: 별도 테이블 password_reset_codes (email, code 6자리, verified, expiredAt 5분, createdAt) — 회원가입 인증 로직과 분리해 회귀 위험 최소화
- 흐름:
  - send: 미가입 이메일이면 404 USER_NOT_FOUND, 기존 코드 삭제 후 새 코드 발송
  - verify: 코드 검증 → verified=true (프론트 단계 게이팅용)
  - confirm: (email, code, newPassword) 최종 단계에서 코드·만료 재검증 후 비밀번호 변경, 코드 삭제
- 보강 TODO(운영): 코드 평문 대신 해시 저장, 발송 레이트리밋, 시도 횟수 제한/잠금 (현재는 email_verification_codes와 동일하게 평문·무제한)

## 회원탈퇴 데이터 정리 전략 (구현 시 결정)

탈퇴 요청 처리와 연관 데이터(게시글/댓글/좋아요 등) 삭제를 분리해 요청 부하를 낮춘다.

- 1차(동기, 즉시 응답): User.status=DELETED, deletedDate 기록, accessToken 무효화 후 204 반환. 조회 단계에서 DELETED 유저 데이터는 필터링 → 사용자 입장에선 즉시 탈퇴.
- 2차(비동기, 연관 데이터 실제 삭제) 옵션:
  - SQS 이벤트 + 워커 (권장, 확장성): "user-deleted" 발행 → 워커가 도메인별 배치 삭제, 실패 시 DLQ 재시도
  - 배치 잡 (간단): DELETED 유저를 주기적으로 스캔해 청크 삭제, 별도 인프라 최소
  - 앱 내 @Async (소규모/임시): 요청 스레드와 분리하나 인스턴스 종속 → 배포/장애 시 유실 위험, 대량 부적합
- 대량 삭제는 청크(LIMIT N) 단위 반복으로 락/replica 지연 완화
- 보관 의무 데이터는 삭제 대신 익명화, 유예기간(복구) 정책 검토 (soft delete면 자연스럽게 지원)
- 권장 조합: soft delete 즉시 응답 + SQS 워커의 도메인별·청크 삭제 (규모 작으면 배치 잡으로 시작)

## 커뮤니티 게시판 (PMS)

### 데이터 모델 결정: 하이브리드 (단일 + section 구분)

섹션(기술/투자/정치)을 표현하는 방식으로 단일 테이블 / 섹션별 분리 테이블 / 하이브리드를 검토 → 하이브리드 채택.

- post (단일 + section 컬럼): 공통 필드(id, section, user_id, title, content, like_count, comment_count, created_at)
  - 섹션별 전용 필수 필드(투자: 종목코드/수익률 등)는 1:1 확장 테이블로 분리 (예: invest_post_detail). 추후 해당 섹션 구현 시 추가
- 분리 테이블 대신 단일을 택한 이유:
  - 댓글/좋아요/저장/신고 등 상호작용은 섹션 무관하게 동일 → 자식 테이블·로직을 section마다 3벌로 복제하지 않음
  - 섹션별 페이지(/tech 등)와 내 글/저장한 글/어드민 서브페이지는 모두 WHERE section=? 로 처리 가능
  - "전용 필드만" 갈리므로 그 부분만 확장 테이블로 분리(하이브리드)
- 성능: WHERE section=? 무차별 스캔 우려 → 복합 인덱스 (section, id)로 인덱스 범위 스캔. 섹션 피드 쿼리는 전체 행수와 무관하게 빠름. 페이지네이션은 cursor(keyset) 사용, OFFSET 지양. 더 커지면 section/시간 파티셔닝 검토
  - 커서는 id-only(legacy 방식과 일치): id가 auto_increment라 작성순=시간순 → id desc만으로 최신순. (section, id) 인덱스로 필터+정렬+커서를 한 번에 커버. created_at 복합 커서는 정렬 기준이 id와 어긋날 때(인기순 등)나 정렬 키가 비유니크일 때만 필요 — 단순 최신순 피드엔 과함
  - 종료 판정도 legacy 방식: +1/hasNext/count 없이 "마지막 글 id를 항상 nextCursor로, 빈 결과면 null". 클라이언트(TanStack useInfiniteQuery)는 nextCursor 유무로만 판단

### 카테고리: 동적 관리 (DB + 어드민 CRUD) — cms(contents) 소유

- 게시판마다 다른 분류 체계(기술: Frontend/Backend…, 투자: 국내주식… )를 코드 상수가 아닌 DB로 관리
- 소유: 카테고리 마스터(정의·CRUD·조회)는 cms(contents) 도메인. 어드민이 관리하는 메타데이터 성격 + 추후 기획전/배너와 같은 곳에서 관리
- categories (단일 + section): id, section, name, sort_order, is_active, created_at, UNIQUE(section, name)
  - 갈리는 컬럼이 없으므로 section별 분리 테이블 X. section 컬럼으로만 구분
- 조회: GET /api/v1/cms/{section}/categories → 어드민 CRUD는 /api/v1/cms/admin/{section}/categories
- 어드민이 추가/삭제/조회 → 프론트는 API로 카테고리를 조회해 section별 필터 칩을 동적 렌더
- post_category_mapping (N:M): post_id, category_id. pms(게시글) 소유. 글당 최대 3개는 앱에서 검증
- 게시글 작성 시 categoryIds 검증은 pms 책임 → 모놀리식은 categories 직접 조회, 분리 후엔 CategoryQueryClient 인터페이스를 cms HTTP 호출 구현으로 교체. 카테고리는 거의 정적이라 캐시로 비용 낮음
- 어드민 카테고리 삭제 시 기존 글 처리(is_active 비활성 vs post_category_mapping 제거)는 어드민 기능 구현 시 결정

### section은 ENUM (테이블화 보류)

- section은 페이지·라우트·전용필드와 코드에 강하게 결합 → row 추가만으로 동작하는 페이지가 생기지 않음(categories와 다른 점)
- 따라서 sections 테이블 대신 애플리케이션 Section enum + VARCHAR(20) 저장(UserType/UserStatus와 동일 패턴)
- 섹션 표시명/테마색/정렬 등 메타데이터 관리 욕구가 생기면 그때 sections 테이블 도입 검토

### count / comment 경계

- like_count, comment_count는 현재 post 컬럼으로 동기 보관. /api/v1/count 경로는 예약만, 추후 count 서비스로 이관
- 댓글은 comment 도메인 예정(/api/v1/comments). community_comments.post_id는 FK 없이 인덱스 컬럼 + app-level 검증(분리 대비)

## 검색 시스템 (search, 추후 도입)

번개장터 구조(bun-search-api-v2: OpenSearch 검색 API, bun-search-indexer: 인덱싱 워커) 참고. search api 구현 시 이 내용을 다시 검토.

### 프론트의 pms vs search 호출 기준 (번개장터 관찰 규칙)
- pms 호출: 단건 정확 조회(id로 1건), 쓰기(작성/수정/삭제/상태변경), "내 것"(개인화+권한 필요) 목록(예: 내 글/찜)
- search 호출: 공개 다건 탐색(키워드 검색, 카테고리/섹션별 목록), 필터·정렬·집계, 자동완성·인기검색어
- 한 문장: "특정 1건을 정확히 보거나 / 쓰거나 / 내 것" → pms, "여러 건을 검색·필터·정렬해서 발견" → search
- 같은 화면 패턴: 검색 결과 목록 = search, 항목 클릭 후 상세 = pms (출처가 다름)
- 구분 방식: 단일 게이트웨이 + 경로 prefix (/api/.../pms vs /api/.../search)

### 구조: CQRS (쓰기 모델 / 읽기 모델 분리)
- pms = 쓰기 원본(Source of Truth, MySQL). 권한 검증·개인화 담당
- search = 읽기 최적화(OpenSearch 인덱스). 검색·필터·정렬·집계 전용(읽기 전용)
- 동기화: pms 쓰기 → 변경 이벤트(SQS/Kinesis 등) → indexer가 받아 MySQL에서 상세 읽어 문서 변환 → OpenSearch 인덱싱 → search-api가 읽음. 실시간이되 최종 일관성(약간 지연)
- count 이벤트화(좋아요/조회수)와 같은 결의 이벤트 파이프라인

### yologram 적용 방향
- 현재 pms 유지: 게시글 작성/수정/삭제·단건 상세, 내 글 목록(개인화+권한)
- 추후 search로 이관: 섹션 피드(공개 목록), 카테고리 필터, 키워드 검색 — 번개장터처럼 "공개 다건 탐색"은 search로 모음
- 개인화+권한 다건(내 글/저장한 글)은 pms 유지(번개 my-shop 방식)
- 단계 전략: 초기엔 pms 목록 API(cursor 페이지네이션)로 시작 → 검색·복잡 필터·대량 트래픽이 실제로 필요해질 때 OpenSearch + indexer 도입(YAGNI). 단일 테이블 + (section, id) 인덱스로 충분한 단계는 pms가 처리
- 별도 서비스 예정(yologram-search-api, yologram-search-indexer), 경로 /api/v1/search/... 또는 게이트웨이 분기

### QueryDSL vs search 역할 구분 (혼동 주의)
- 둘 다 "복잡 조회"지만 복잡함의 축이 다름:
  - QueryDSL(pms, RDB): 관계형 복잡성 — 동적 조건(WHERE 조합), 다중 조인, projection(필요 필드만), 조건부 정렬, 페이지네이션, 벌크 update. uid·권한으로 한정된 "내 것/정확" 조회.
  - search(OpenSearch): 탐색 복잡성 — 풀텍스트(형태소·유사어), 연관도 점수 정렬, 패싯 집계. 공개 카탈로그를 "발견".
- 같은 다건·필터라도: 공개 카탈로그 탐색은 search, uid/권한으로 한정된 내 데이터 정밀 조회는 pms+QueryDSL (번개장터: 내 상품/내 찜은 QueryDSL, 공개 상품 검색은 search)

### api-v1에서 QueryDSL을 쓰는 기준
- 단순 단건·고정 조건(findBy/existsBy)·기본 CRUD → Spring Data JpaRepository로 충분
- 다음 중 하나라도 해당하면 QueryDSL custom repository 사용:
  1. 동적 조건 — 선택적 필터(있을 수도 없을 수도)를 런타임에 조합 (BooleanBuilder/BooleanExpression)
  2. 다중 조인 — 2개 이상 엔티티 조인
  3. projection — 엔티티 전체가 아니라 필요한 컬럼만 DTO로 select
  4. 조건부 정렬 / cursor·offset 페이지네이션
  5. 벌크 update·delete (조건부 여러 행 변경)
- yologram 예: "내 글 목록"(section·작성자 필터 + 카테고리 조인 + cursor 페이지네이션) = pms + QueryDSL / 공개 섹션 피드·키워드 검색 = search
- 구현 메모: 게시글 목록 API(PostRepositoryImpl.findPostsBySection)가 프로젝트 첫 QueryDSL 사용처 — 동적 조건(categoryId/cursor) + EXISTS 서브쿼리 + keyset. JPAQueryFactory 빈은 QuerydslConfig
- 주의(QueryDSL 함정): enum을 담는 패키지명에 `enum`(Java 예약어) 사용 금지. QueryDSL APT는 Java 코드를 생성하는데 `import ...enum.Xxx`를 만들지 못해 해당 enum 필드가 Q클래스에서 통째로 누락됨(다른 타입 필드는 정상). cms.enum → cms.enums로 변경해 해결. ums.enum도 동일 잠재 이슈(현재 QueryDSL 미사용이라 보류)
