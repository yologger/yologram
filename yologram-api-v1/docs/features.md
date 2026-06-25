# yologram-api-v1 구현 기능 및 설계 근거

구현 완료된 기능과 그 설계 근거를 기록한다. (앞으로 할 일은 tasks.md)

## 구현된 기능

### UMS (회원/인증) — /api/v1/ums
- 회원가입 POST /user/join (이메일 인증 필수)
- 로그인 POST /auth/login, 로그아웃 POST /auth/logout, 토큰 검증 POST /auth/validate-token
- 회원정보 조회 GET /user/me, 수정 PATCH /user/me, 비밀번호 변경 PATCH /user/me/password
- 이메일 인증 POST /auth/email-verification/send·verify (AWS SES)
- 비밀번호 찾기 POST /auth/password-reset/send·verify·confirm
- 회원탈퇴 DELETE /user/me (현재 하드 삭제)

### CMS (카테고리) — /api/v1/cms
- 카테고리 조회 GET /{section}/categories (is_active=true, sort_order 정렬)

### PMS (게시글) — /api/v1/pms
- 작성 POST /{section}/posts (인증, categoryIds 1~3 검증)
- 상세 GET /{section}/posts/{id} (공개)
- 목록 GET /{section}/posts (공개, id desc cursor 페이지네이션, categoryId 필터) — 프로젝트 첫 QueryDSL 사용처

### 인프라/공통
- R/W splitting(MasterSlaveRoutingDataSource), Testcontainers 통합 테스트, Swagger
- Observability: Grafana Cloud OTLP direct push (logs/metrics/traces)

## 설계 근거

### 도메인 구조 (모듈러 모놀리식 → MSA 대비)
- 단일 서버에서 도메인별 패키지로 분리. 도메인 경계 = 미래 MSA 경계 = API Gateway path prefix
  - UMS→yologram-user-api, PMS→yologram-post-api, CMS→yologram-cms-api, Comment→comment-api, Count→count-api, News→news-api
- 분리 전략: 도메인 간 직접 JOIN·repository 교차 호출 금지
  - 경계 호출은 인터페이스(QueryClient)로 추상화 → 분리 시 HTTP/gRPC 구현으로 교체 (예: CategoryQueryClient, UserQueryClient)
  - self HTTP 호출 지양. 모놀리식 단계는 직접 호출 구현
  - 경계 넘는 FK 지양(같은 도메인 내부만 FK). 경계 넘는 참조는 인덱스 + app-level 검증
  - 경계 넘는 동기 트랜잭션 의존 최소화 (count 갱신은 추후 이벤트/최종일관성)

### API 경로 규칙
- /api/v1/ums/ (유저), /api/v1/pms/{section}/ (게시글), /api/v1/cms/{section}/categories (카테고리)
- /api/v1/comments/ (댓글, 추후), /api/v1/count/ (카운트, 예약), /api/v1/news/ (뉴스, 추후)
- 어드민: 도메인 경로 뒤 admin 세그먼트 → /api/v1/{domain}/admin/... (게이트웨이 라우팅과 일관)

### 유저 타입
- DEFAULT(일반), POLITICIAN(정치인), ECONOMIST(경제인), ADMIN(관리자)

### 인증 방식
- JWT(HMAC256), Authorization: Bearer 헤더
- access token은 stateless (서버 미저장). 다중 로그인 지원 위해 DB 토큰 비교 제거 → 로그아웃은 클라이언트 토큰 폐기, 서버측 강제 무효화는 refresh token 도입 시 함께 구현
- validate-token은 로그인 직후 replica lag 회피 위해 master DB 조회

### 이메일 인증
- EmailSender 인터페이스로 발송 추상화 (개발: StubEmailSender 로그, 프로덕션: SesEmailSender)
- user_email_verification 테이블(email, code 6자리, verified, expiredAt). 코드 발송→검증(verified=true)→가입 시 확인→가입 후 삭제. 재발송 시 기존 삭제 후 생성

### 비밀번호 찾기
- 이메일 6자리 코드 발송→검증→재설정 (이메일 인증과 동일 패턴/SES 재사용)
- 별도 테이블 user_password_reset_code (회원가입 인증 로직과 분리해 회귀 위험 최소화)
- send(미가입 404, 기존 코드 삭제 후 발송) → verify(verified=true, 프론트 게이팅) → confirm(email·code·newPassword 재검증 후 변경·코드 삭제)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한 (현재 평문·무제한)

### 회원탈퇴 데이터 정리 전략 (soft delete 전환 시 결정)
- 탈퇴 요청과 연관 데이터(게시글/댓글/좋아요) 삭제를 분리해 요청 부하 완화
- 1차(동기): status=DELETED + deletedDate 기록, 토큰 무효화 후 204. 조회 시 DELETED 필터링 → 즉시 탈퇴 효과
- 2차(비동기 연관 삭제): SQS 이벤트+워커(권장) / 배치 잡(간단) / 앱 @Async(소규모, 유실 위험)
- 대량 삭제는 청크(LIMIT N) 반복으로 락·replica 지연 완화. 보관 의무 데이터는 익명화·유예기간 검토
- 권장: soft delete 즉시 응답 + SQS 워커 도메인별 청크 삭제 (규모 작으면 배치 잡으로 시작)

### 커뮤니티 게시글 데이터 모델: 하이브리드 (단일 + section)
- post (단일 테이블 + section 컬럼): 공통 필드(id, section, user_id, title, content, like_count, comment_count, created_at)
  - 섹션별 전용 필수 필드(투자 종목코드/수익률 등)는 1:1 확장 테이블로 분리(예: invest_post_detail), 해당 섹션 구현 시 추가
- 단일 테이블 채택 이유: 댓글/좋아요/저장/신고 등 상호작용이 섹션 무관 동일 → 자식 테이블·로직을 section마다 복제 안 함. 섹션 페이지·내 글·어드민은 모두 WHERE section=?로 처리. "전용 필드만" 갈리니 그 부분만 확장 테이블(하이브리드)
- 성능: 복합 인덱스 (section, id) = idx_post_section_id로 범위 스캔. 페이지네이션은 cursor(keyset), OFFSET 지양. 더 커지면 파티셔닝 검토

### 커서 페이지네이션: id-only (legacy 방식과 일치)
- id가 auto_increment라 작성순=시간순 → id desc만으로 최신순. (section, id) 인덱스로 필터+정렬+커서를 한 번에 커버
- created_at 복합 커서는 정렬 기준이 id와 어긋날 때(인기순 등)나 정렬 키가 비유니크일 때만 필요 — 단순 최신순 피드엔 과함
- 종료 판정: +1/hasNext/count 없이 "마지막 글 id를 항상 nextCursor로, 빈 결과면 null". 클라이언트(TanStack useInfiniteQuery)는 nextCursor 유무로만 판단

### 카테고리: 동적 관리 (DB + 어드민 CRUD) — cms 소유
- 게시판마다 다른 분류 체계를 코드 상수가 아닌 DB로 관리. 카테고리 마스터는 cms 도메인(어드민 관리 메타데이터)
- post_category 테이블(id, section, name, sort_order, is_active, created_at, UNIQUE(section, name)). 갈리는 컬럼 없어 section별 분리 X
- post_category_mapping (N:M): post_id, category_id. pms 소유. 글당 최대 3개 앱 검증
- 작성 시 categoryIds 검증은 pms 책임 → 모놀리식은 post_category 직접 조회, 분리 후 PostCategoryQueryClient를 cms HTTP 호출 구현으로 교체 (카테고리는 거의 정적이라 캐시 가능)
- 어드민 카테고리 삭제 시 기존 글 처리(is_active 비활성 vs 매핑 제거)는 어드민 기능 구현 시 결정

### section은 ENUM (테이블화 보류)
- section은 페이지·라우트·전용필드와 코드에 강하게 결합 → row 추가만으로 동작하는 페이지가 안 생김(categories와 다른 점)
- sections 테이블 대신 Section enum + VARCHAR(20) 저장(UserType/UserStatus와 동일 패턴). 표시명/테마색 등 메타데이터 욕구 생기면 그때 테이블 도입 검토

### count / comment 경계
- like_count, comment_count는 현재 post 컬럼 동기 보관. /api/v1/count 경로는 예약만, 추후 count 서비스로 이관
- 댓글은 comment 도메인 예정(/api/v1/comments). community_comments.post_id는 FK 없이 인덱스 + app-level 검증(분리 대비)

### QueryDSL 사용 기준 (api-v1)
- 단순 단건·고정 조건(findBy/existsBy)·기본 CRUD → Spring Data JpaRepository로 충분
- 다음 중 하나라도 해당하면 QueryDSL custom repository: ①동적 조건 ②다중 조인(2개+ 엔티티) ③projection(필요 컬럼만 DTO) ④조건부 정렬/cursor·offset 페이지네이션 ⑤벌크 update·delete
- yologram 예: "내 글 목록"(section·작성자 필터 + 카테고리 조인 + 페이지네이션) = pms + QueryDSL / 공개 섹션 피드·키워드 검색 = search
- 구현 메모: PostRepositoryImpl.findPostsBySection이 첫 QueryDSL 사용처 — 동적 조건(categoryId/cursor) + EXISTS 서브쿼리 + keyset. JPAQueryFactory 빈은 QuerydslConfig
- 함정: enum을 담는 패키지명에 `enum`(Java 예약어) 금지. QueryDSL APT가 `import ...enum.Xxx`를 생성 못 해 해당 enum 필드가 Q클래스에서 통째 누락됨(다른 타입 필드는 정상). cms.enum → cms.enums로 해결. ums.enum도 동일 잠재 이슈(현재 QueryDSL 미사용이라 보류)

### 검색 시스템 (search, 추후 도입) — 번개장터 구조 참고
- pms vs search 호출 기준: 단건 정확 조회·쓰기·"내 것"(개인화+권한) = pms / 공개 다건 탐색(키워드·카테고리·섹션 목록·필터·정렬·집계) = search
- CQRS: pms = 쓰기 원본(MySQL, 권한·개인화) / search = 읽기 최적화(OpenSearch). 동기화는 pms 쓰기 → 변경 이벤트(SQS/Kinesis) → indexer가 MySQL 읽어 문서화 → OpenSearch 인덱싱 (최종 일관성)
- QueryDSL vs search 역할: QueryDSL은 관계형 복잡성(권한 한정 "내 것/정확"), search는 탐색 복잡성(풀텍스트·연관도·패싯, 공개 카탈로그 발견)
- 도입 전략: 초기엔 pms cursor 목록으로 시작 → 검색·복잡 필터·대량 트래픽 필요 시 OpenSearch+indexer 도입(YAGNI). 별도 서비스 예정(yologram-search-api, yologram-search-indexer)
