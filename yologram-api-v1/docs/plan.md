# yologram-api-v1 구현 계획

## 1단계: 회원가입

### 의존성
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-security-crypto (BCryptPasswordEncoder)
- mysql-connector-j
- java-jwt (Auth0)
- querydsl (kapt)
- testcontainers (MySQL)

### DB 설정
- application.yaml: JPA/Hibernate 공통 (ddl-auto, batch-size, timezone)
- application-local.yaml: 로컬 MySQL (testcontainers 또는 로컬 DB)
- application-prod.yaml: RDS MySQL (Parameter Store 주입)
- R/W splitting: MasterSlaveRoutingDataSource

### 엔티티
- User: id, email, name, nickname, password, avatar, accessToken, type, status, deletedDate, joinedDate, modifiedDate

### API
- POST /api/v1/ums/user/join → 회원가입

### 테스트
- UserService 단위 테스트
- 회원가입 API 통합 테스트 (Testcontainers)

## 2단계: 로그인/로그아웃

### API
- POST /api/v1/ums/auth/login
- POST /api/v1/ums/auth/logout
- POST /api/v1/ums/auth/validate-token (master DB 조회)

### JWT
- HMAC256, Auth0 java-jwt
- 토큰 생성/검증 유틸
- 설정값: application.yaml (secret, expire, issuer, audience)

### 테스트
- AuthService 단위 테스트
- 로그인/로그아웃 API 통합 테스트

## 3단계: 회원정보 조회

### API
- GET /api/v1/ums/user/me (본인 정보 조회, 인증 필요)

### 테스트
- UserService 단위 테스트
- 회원정보 조회 API 통합 테스트

## 4단계: 회원정보 수정

### API
- PATCH /api/v1/ums/user/me (이름, 닉네임 변경, 인증 필요)

### 테스트
- UserService 단위 테스트
- 회원정보 수정 API 통합 테스트

## 5단계: 비밀번호 변경

### API
- PATCH /api/v1/ums/user/me/password (현재 비밀번호 + 새 비밀번호, 인증 필요)

### 테스트
- 비밀번호 변경 서비스 단위 테스트
- 비밀번호 변경 API 통합 테스트

## 6단계: 이메일 인증 (AWS SES)

### 흐름
- 회원가입 전 이메일로 6자리 인증 코드 발송
- 사용자가 인증 코드 입력 → 검증 통과 (verified = true)
- 회원가입 시 해당 이메일의 verified 확인 → 가입 완료 → 인증 레코드 삭제
- email_verification_codes 테이블에 코드 저장 (5분 만료)

### 구조
- EmailSender 인터페이스로 발송 추상화
- StubEmailSender: 로그 출력 (개발/테스트용)
- SesEmailSender: AWS SES 연동 (HTML 이메일 발송)

### API
- POST /api/v1/ums/auth/email-verification/send (이메일 중복 확인 → 코드 생성 → 발송)
- POST /api/v1/ums/auth/email-verification/verify (코드 확인 + 만료 체크 → verified = true)

### 예외
- EmailVerificationExpiredException (400): 코드 만료
- EmailVerificationInvalidException (400): 코드 불일치 또는 레코드 없음
- EmailNotVerifiedException (400): 미인증 상태로 회원가입 시도

### 테스트
- EmailVerificationService 단위 테스트
- AuthResource 슬라이스 테스트 (발송/인증 엔드포인트)
- UserService 이메일 인증 연동 테스트 (미인증 가입 차단)

## 7단계: 비밀번호 찾기 (AWS SES)

### 흐름
- 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (회원가입 이메일 인증과 동일 패턴)
- 저장: password_reset_codes 테이블 (5분 만료), confirm 시 코드 재검증 후 변경·삭제

### API
- POST /api/v1/ums/auth/password-reset/send (미가입 시 404, 코드 발송)
- POST /api/v1/ums/auth/password-reset/verify (코드 검증 → verified)
- POST /api/v1/ums/auth/password-reset/confirm (email, code, newPassword → 재검증 후 변경)

### 예외
- UserNotFoundException (404): 미가입 이메일
- PasswordResetExpiredException (400): 코드 만료
- PasswordResetInvalidException (400): 코드 불일치

## 8단계: Refresh Token

### 흐름
- login 시 access token + refresh token 쌍 발급
- access token 만료 시 refresh token으로 재발급

### API
- POST /api/v1/ums/auth/refresh

## 회원탈퇴

### 현재 (개발 단계: 하드 삭제)
- DELETE /api/v1/ums/user/me: 유저 레코드를 즉시 삭제 → email 해제로 재가입 가능
- 단순화를 위해 개발 기간 임시 동작

### 추후 (b 방식: soft delete + 유예 후 정리)
- status=DELETED + deletedDate, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403)
- 유예기간 후 PII 익명화/하드삭제 배치, 연관 데이터(게시글 등) 비동기 정리
- 조회 시 DELETED 필터링, email 재가입 정책

## 커뮤니티 (PMS / CMS)

### 데이터 모델 (하이브리드: 단일 + section)
- community_posts: id, section, user_id, title, content, like_count, comment_count, created_at, modified_date
  - 인덱스 (section, created_at) — 섹션 피드 cursor 페이지네이션
  - 섹션 전용 필드는 추후 1:1 확장 테이블(예: invest_post_detail)
- categories: id, section, name, sort_order, is_active, created_at, UNIQUE(section, name) — 동적 관리(어드민 CRUD)
- post_categories: post_id, category_id (N:M, 글당 최대 3개)
- Section enum: TECH / INVEST / POLITICS (VARCHAR 저장)

### 도메인/경로
- domain/cms (Category, contents), domain/pms (Post + post_categories)
- 경로 path 변수 방식: 카테고리 /api/v1/cms/{section}/categories, 게시글 /api/v1/pms/{section}/posts
- 어드민은 도메인 경로 뒤 admin 세그먼트: /api/v1/cms/admin/{section}/categories 등
- 모듈러 모놀리식 — 도메인 경계 호출은 인터페이스 추상화, 경계 넘는 FK 지양

### 1단계: 카테고리 조회 (cms, 먼저)
- GET /api/v1/cms/{section}/categories → is_active=true, sort_order 정렬
- Category 엔티티 + CategoryRepository + CategoryService + CategoryResource(컨트롤러)
- section 유효성 검증(잘못된 section → 400)
- 시드: TECH(Frontend/Backend/AI·ML/DevOps/Cloud/Security/기타), INVEST/POLITICS
- 프론트 연동: techCategories 상수 → API 조회로 대체, section별 필터 칩 동적 렌더

### 2단계: 게시글 작성 (pms)
- POST /api/v1/pms/{section}/posts (인증 필요) { title?, content, categoryIds[] }
- 작성자 = JWT 인증 유저, categoryIds가 해당 section 카테고리인지 검증(모놀리식: categories 직접 조회) → community_posts + post_categories insert
- 응답 { id } (201)

### 3단계: 게시글 조회 (구현됨)
- GET /api/v1/pms/{section}/posts/{id} (상세, 공개)
- GET /api/v1/pms/{section}/posts (목록, 공개) — 최신순(id desc) + cursor(keyset) 페이지네이션, categoryId 필터(옵션), size 기본 20·최대 50
- 커서는 id-only(legacy yologram-legacy 방식): 마지막 글 id를 nextCursor로, 빈 결과면 null. ApiEnvelopCursorPage{data, nextCursor}
- QuerydslConfig(JPAQueryFactory) 도입 — 프로젝트 첫 QueryDSL. 인덱스 (section, id)

### 4단계 이후 (예정)
- 게시글 수정 / 삭제
- 댓글(comment 도메인 /api/v1/comments, post_id FK 없음) / 좋아요·카운트(count 경로 예약)
- 어드민 카테고리 CRUD(/api/v1/cms/admin/...), 어드민 게시글 관리

## 제외 범위 (이후)

- OAuth (Gmail, Kakao)
- 프로필 이미지 업로드
