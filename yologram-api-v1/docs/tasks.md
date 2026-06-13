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

## UMS - 비밀번호 찾기 (7단계)

- [ ] POST /api/v1/ums/auth/reset-password (이메일로 비밀번호 재설정 링크/임시 비밀번호 발송)
- [ ] 비밀번호 재설정 처리 로직
- [ ] 비밀번호 찾기 테스트
- [ ] Swagger 문서화

## UMS - Refresh Token (8단계)

- [ ] refresh token 발급 로직 (login 시 access + refresh 쌍 발급)
- [ ] POST /api/v1/ums/auth/refresh (refresh token으로 access token 재발급)
- [ ] refresh token 저장/검증 로직
- [ ] refresh token 테스트
- [ ] Swagger 문서화

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
