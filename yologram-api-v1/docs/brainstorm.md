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
  - PMS (Post Management System): 커뮤니티 게시글 + post_categories(연결) → yologram-post-api
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

- community_posts (단일 + section 컬럼): 공통 필드(id, section, user_id, title, content, like_count, comment_count, created_at)
  - 섹션별 전용 필수 필드(투자: 종목코드/수익률 등)는 1:1 확장 테이블로 분리 (예: invest_post_detail). 추후 해당 섹션 구현 시 추가
- 분리 테이블 대신 단일을 택한 이유:
  - 댓글/좋아요/저장/신고 등 상호작용은 섹션 무관하게 동일 → 자식 테이블·로직을 section마다 3벌로 복제하지 않음
  - 섹션별 페이지(/tech 등)와 내 글/저장한 글/어드민 서브페이지는 모두 WHERE section=? 로 처리 가능
  - "전용 필드만" 갈리므로 그 부분만 확장 테이블로 분리(하이브리드)
- 성능: WHERE section=? 무차별 스캔 우려 → 복합 인덱스 (section, created_at)로 인덱스 범위 스캔. 섹션 피드 쿼리는 전체 행수와 무관하게 빠름. 페이지네이션은 cursor(keyset) 사용, OFFSET 지양. 더 커지면 section/시간 파티셔닝 검토

### 카테고리: 동적 관리 (DB + 어드민 CRUD) — cms(contents) 소유

- 게시판마다 다른 분류 체계(기술: Frontend/Backend…, 투자: 국내주식… )를 코드 상수가 아닌 DB로 관리
- 소유: 카테고리 마스터(정의·CRUD·조회)는 cms(contents) 도메인. 어드민이 관리하는 메타데이터 성격 + 추후 기획전/배너와 같은 곳에서 관리
- categories (단일 + section): id, section, name, sort_order, is_active, created_at, UNIQUE(section, name)
  - 갈리는 컬럼이 없으므로 section별 분리 테이블 X. section 컬럼으로만 구분
- 조회: GET /api/v1/cms/{section}/categories → 어드민 CRUD는 /api/v1/cms/admin/{section}/categories
- 어드민이 추가/삭제/조회 → 프론트는 API로 카테고리를 조회해 section별 필터 칩을 동적 렌더
- post_categories (N:M): post_id, category_id. pms(게시글) 소유. 글당 최대 3개는 앱에서 검증
- 게시글 작성 시 categoryIds 검증은 pms 책임 → 모놀리식은 categories 직접 조회, 분리 후엔 CategoryQueryClient 인터페이스를 cms HTTP 호출 구현으로 교체. 카테고리는 거의 정적이라 캐시로 비용 낮음
- 어드민 카테고리 삭제 시 기존 글 처리(is_active 비활성 vs post_categories 제거)는 어드민 기능 구현 시 결정

### section은 ENUM (테이블화 보류)

- section은 페이지·라우트·전용필드와 코드에 강하게 결합 → row 추가만으로 동작하는 페이지가 생기지 않음(categories와 다른 점)
- 따라서 sections 테이블 대신 애플리케이션 Section enum + VARCHAR(20) 저장(UserType/UserStatus와 동일 패턴)
- 섹션 표시명/테마색/정렬 등 메타데이터 관리 욕구가 생기면 그때 sections 테이블 도입 검토

### count / comment 경계

- like_count, comment_count는 현재 community_posts 컬럼으로 동기 보관. /api/v1/count 경로는 예약만, 추후 count 서비스로 이관
- 댓글은 comment 도메인 예정(/api/v1/comments). community_comments.post_id는 FK 없이 인덱스 컬럼 + app-level 검증(분리 대비)
