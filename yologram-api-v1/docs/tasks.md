## 인프라

- [ ] ECS 헬스체크 설정: actuator 의존성 추가 + Task Definition에 healthCheck 설정
- [x] GitHub Actions job timeout 설정

## Observability

- [x] Grafana Cloud 연동 - Logs: OTLP (opentelemetry-logback-appender)
- [x] Grafana Cloud 연동 - Traces: OTLP (micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp)
- [x] Grafana Cloud 연동 - Metrics: OTLP (micrometer-registry-otlp)

## DB 설정

- [x] 의존성 추가 (JPA, MySQL, QueryDSL, Testcontainers)
- [x] R/W splitting 구성 (MasterSlaveRoutingDataSource)
- [x] application.yaml JPA/Hibernate 설정
- [x] application-local.yaml 로컬 DB 설정
- [x] application-prod.yaml RDS 설정 (Parameter Store)

## UMS - 회원가입 (1단계)

- [x] User 엔티티 (email, name, nickname, password, avatar, accessToken, type, status)
- [x] UserType enum (DEFAULT, POLITICIAN, ECONOMIST, ADMIN)
- [x] UserStatus enum (ACTIVE, INACTIVE, DELETED)
- [x] UserRepository
- [x] BCryptPasswordEncoder 설정
- [x] UserService.join()
- [x] POST /api/v1/ums/user/join 컨트롤러
- [x] UserDuplicateException 예외 처리
- [x] 회원가입 단위 테스트
- [x] 회원가입 통합 테스트 (Testcontainers)

## API 설정

- [x] Swagger UI 경로: /api/v1/docs
- [x] api-docs 경로: /api/v1/api-docs
- [x] CORS 전체 허용 (WebConfig)

## UMS - 로그인/로그아웃 (2단계)

- [x] JwtProperties (secret, expire, issuer, audience)
- [x] JwtUtil (토큰 생성, 검증, uid 추출)
- [x] AuthService (login, logout, validateToken)
- [x] POST /api/v1/ums/auth/login
- [x] POST /api/v1/ums/auth/logout
- [x] POST /api/v1/ums/auth/validate-token
- [x] @AuthenticatedUser 리졸버 (Authorization: Bearer 헤더)
- [x] 로그아웃 후 토큰 무효화 (DB accessToken 비교)
- [x] validate-token master DB 조회 (로그인 직후 replica lag 방지)
- [x] JwtUtil 단위 테스트
- [x] AuthService 단위 테스트
- [x] AuthResource 슬라이스 테스트

## UMS - 회원정보 조회 (3단계)

- [x] GET /api/v1/ums/user/me (본인 정보 조회)
- [x] 회원정보 조회 테스트
- [x] Swagger 문서화

## UMS - 회원정보 수정 (4단계)

- [x] PATCH /api/v1/ums/user/me (닉네임 변경)
- [x] 회원정보 수정 테스트
- [x] Swagger 문서화

## UMS - 비밀번호 변경 (5단계)

- [x] PATCH /api/v1/ums/user/me/password (현재 비밀번호 + 새 비밀번호)
- [x] 비밀번호 변경 테스트
- [x] Swagger 문서화

## UMS - 이메일 인증 (6단계)

- [x] AWS SES 연동 (SesEmailSender, 프로필 분기)
- [x] EmailVerificationCode 엔티티 (email, code, verified, expiredAt)
- [x] EmailVerificationCodeRepository
- [x] EmailSender 인터페이스 + StubEmailSender (로그 출력)
- [x] EmailVerificationService (sendCode, verifyCode)
- [x] POST /api/v1/ums/auth/email-verification/send
- [x] POST /api/v1/ums/auth/email-verification/verify
- [x] 회원가입 시 이메일 인증 필수화 (UserService.join)
- [x] 예외 처리 (EmailVerificationExpired/Invalid, EmailNotVerified)
- [x] EmailVerificationService 단위 테스트 (7개)
- [x] AuthResource 슬라이스 테스트 (9개)
- [x] UserService 이메일 인증 연동 테스트 (3개)
- [x] Swagger 문서화

## UMS - 회원탈퇴

- [x] DELETE /api/v1/ums/user/me (본인 탈퇴, 개발 단계: 레코드 하드 삭제 → email 즉시 해제·재가입 가능)
- [x] 회원탈퇴 테스트
- [x] Swagger 문서화
- [ ] (추후) soft delete 방식 전환: status=DELETED + deletedDate, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403)
- [ ] (추후) 유예기간 후 PII 익명화/하드삭제 배치, email 재가입 정책
- [ ] (추후) 연관 데이터 정리 비동기 처리 (게시글 등, 이벤트/큐 기반) — 게시글 도메인 추가 후
- [ ] (추후) 조회 시 DELETED 유저 데이터 필터링

## UMS - 비밀번호 찾기 (7단계)

- [x] PasswordResetCode 엔티티 + password_reset_codes 테이블
- [x] PasswordResetCodeRepository
- [x] EmailSender.sendPasswordResetCode (Stub/Ses 구현)
- [x] PasswordResetService (sendCode, verifyCode, confirm)
- [x] POST /api/v1/ums/auth/password-reset/send (미가입 시 404)
- [x] POST /api/v1/ums/auth/password-reset/verify
- [x] POST /api/v1/ums/auth/password-reset/confirm (email, code, newPassword 재검증)
- [x] 예외 (PasswordResetExpired/Invalid)
- [x] 비밀번호 찾기 테스트 (service, AuthResource 슬라이스)
- [x] Swagger 문서화
- [ ] (운영 보강) 코드 해시 저장, 레이트리밋, 시도 횟수 제한
- [ ] (운영 보강) 코드 발송 재발송 최소 간격 제한 (최근 발송 후 N초 이내 429, email-verification·password-reset 공통)

## UMS - Refresh Token (8단계)

- [ ] refresh token 발급 로직 (login 시 access + refresh 쌍 발급)
- [ ] POST /api/v1/ums/auth/refresh (refresh token으로 access token 재발급)
- [ ] refresh token 저장/검증 로직
- [ ] 로그아웃 시 refresh token 폐기 (서버측 토큰 무효화) — 현재 access token은 stateless라 무효화 불가, refresh 도입과 함께 구현
- [ ] refresh token 테스트
- [ ] Swagger 문서화

## CMS - 커뮤니티 카테고리 (1단계)

- [x] Section enum (TECH / INVEST / POLITICS)
- [ ] categories 테이블 DDL + section별 시드 데이터 (DB 직접 실행)
- [x] Category 엔티티 + CategoryRepository
- [x] CategoryService (section별 활성 카테고리 조회, sort_order 정렬)
- [x] GET /api/v1/cms/{section}/categories 컨트롤러
- [x] section 유효성 검증 (잘못된 section → 400)
- [x] 테스트 (정상/빈 목록/유효하지 않은 section, 7개)
- [x] Swagger 문서화
- [ ] 프론트 연동: web-v1·web-v2 techCategories 상수 → API 조회로 대체, section별 필터 동적 렌더

## PMS - 커뮤니티 게시글 작성 (2단계)

- [ ] community_posts 테이블 DDL + 인덱스 (section, created_at)
- [ ] post_categories 테이블 DDL
- [ ] Post / PostCategory 엔티티 + 리포지토리
- [ ] PostService.create (작성자=인증유저, categoryIds section 일치 검증, 최대 3개)
- [ ] POST /api/v1/pms/{section}/posts 컨트롤러 (인증 필요)
- [ ] 예외 처리 (카테고리 section 불일치, 내용 누락, 카테고리 개수 초과)
- [ ] 테스트 (정상/미인증/section 불일치/내용 누락/카테고리 초과)
- [ ] Swagger 문서화

## PMS - 게시글 조회/상세 (3단계, 예정)

- [ ] GET /api/v1/pms/{section}/posts (cursor 페이지네이션)
- [ ] GET /api/v1/pms/{section}/posts/{id} (상세)
- [ ] 카테고리 필터 (categoryId)
- [ ] 테스트 / Swagger

## Comment - 댓글 (예정)

- [ ] community_comments 테이블 (post_id FK 없음, 인덱스 + app-level 검증)
- [ ] 댓글 작성/조회/삭제 API (/api/v1/comments/...)

## Count - 좋아요/카운트 (경로 예약, 예정)

- [ ] 좋아요 토글 (/api/v1/count/... 경로 예약, 현재 community_posts 컬럼 동기 보관)
- [ ] 분리 시 이벤트 기반 카운트 이관

## Admin - 커뮤니티 카테고리 관리 (예정)

- [ ] POST/DELETE/GET /api/v1/cms/admin/{section}/categories (어드민 권한)
- [ ] 카테고리 삭제 시 기존 글 처리 정책 (is_active vs post_categories 제거)

## Admin - 유저 관리

- [ ] GET /api/v1/ums/admin/users (유저 목록 조회)
- [ ] GET /api/v1/ums/admin/users/{uid} (유저 상세 조회)
- [ ] PATCH /api/v1/ums/admin/users/{uid} (유저 정보 수정)
- [ ] DELETE /api/v1/ums/admin/users/{uid} (유저 삭제)
- [ ] 어드민 권한 검증 (UserType.ADMIN)
- [ ] 테스트
- [ ] Swagger 문서화

## Admin - 게시글 관리

- [ ] GET /api/v1/admin/posts (게시글 목록 조회)
- [ ] GET /api/v1/admin/posts/{id} (게시글 상세 조회)
- [ ] DELETE /api/v1/admin/posts/{id} (게시글 삭제)
- [ ] 테스트
- [ ] Swagger 문서화
