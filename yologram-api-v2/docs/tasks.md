## 인프라

- [ ] GitHub Actions 빌드 캐시 적용: Docker 레이어 캐시 (docker/build-push-action)

## Observability

- [x] Grafana Cloud 연동 - Logs: OTLP (opentelemetry-sdk + OTLPLogExporter)
- [x] Grafana Cloud 연동 - Traces: OTLP (opentelemetry-sdk + OTLPSpanExporter)
- [x] Grafana Cloud 연동 - Metrics: OTLP (opentelemetry-sdk + OTLPMetricExporter)

## DB 설정

- [x] 의존성 추가 (sqlalchemy, pymysql)
- [x] Settings에 DB 필드 추가
- [x] SQLAlchemy engine + SessionLocal
- [x] get_db 의존성 함수

## 공통

- [x] ApiEnvelop 응답 래퍼
- [x] 예외 처리 (UserDuplicateException → 409)
- [x] CORS 전체 허용

## UMS - 회원가입

- [x] User 모델 (SQLAlchemy)
- [x] UserType, UserStatus enum
- [x] JoinRequest, JoinResponse schema
- [x] UserRepository
- [x] UserService.join()
- [x] POST /api/v2/ums/user/join router
- [x] 회원가입 테스트 (service, router)

## UMS - 로그인/로그아웃/토큰검증 (2단계)

- [x] Settings에 jwt_secret, jwt_expire, jwt_issuer, jwt_audience 추가
- [x] PyJWT 의존성 추가
- [x] jwt_util.py (create_token, validate_and_get_uid)
- [x] 인증 스키마 (LoginRequest, LoginResponse, ValidateTokenResponse, AuthData)
- [x] 인증 예외 (AuthWrongPasswordException, AuthTokenExpiredException, AuthTokenInvalidException)
- [x] 인증 의존성 (get_authenticated_user - Bearer 토큰 추출/검증)
- [x] AuthService (login, validate_token, logout)
- [x] POST /api/v2/ums/auth/login
- [x] POST /api/v2/ums/auth/validate-token
- [x] POST /api/v2/ums/auth/logout (204)
- [x] jwt_util 단위 테스트 (4개)
- [x] auth_service 단위 테스트 (8개)
- [x] auth_router E2E 테스트 (10개)

## UMS - 회원정보 조회 (3단계)

- [x] GET /api/v2/ums/user/me (본인 정보 조회)
- [x] 회원정보 조회 테스트
- [x] Swagger 문서화

## UMS - 회원정보 수정 (4단계)

- [x] PATCH /api/v2/ums/user/me (닉네임 변경)
- [x] 회원정보 수정 테스트
- [x] Swagger 문서화

## UMS - 비밀번호 변경 (5단계)

- [x] PATCH /api/v2/ums/user/me/password (현재 비밀번호 + 새 비밀번호)
- [x] 비밀번호 변경 테스트
- [x] Swagger 문서화

## UMS - 이메일 인증 (6단계)

- [x] AWS SES 연동 (이메일 발송 서비스, boto3)
- [x] 인증 코드 생성/저장 로직 (email_verification_codes 테이블, 5분 만료)
- [x] POST /api/v2/ums/auth/email-verification/send
- [x] POST /api/v2/ums/auth/email-verification/verify
- [x] 회원가입 시 이메일 인증 필수화
- [x] 이메일 인증 테스트
- [x] Swagger 문서화

## UMS - 회원탈퇴

- [ ] DELETE /api/v2/ums/user/me (본인 탈퇴, soft delete: status=DELETED, deleted_date 기록, access_token 무효화)
- [ ] 관련 데이터 정리 비동기 처리 (게시글 등, 이벤트/큐 기반)
- [ ] 회원탈퇴 테스트
- [ ] Swagger 문서화

## UMS - 비밀번호 찾기 (7단계)

- [x] PasswordResetCode 모델 (password_reset_codes 테이블)
- [x] PasswordResetCodeRepository
- [x] EmailSender.send_password_reset_code (Stub/Ses 구현)
- [x] PasswordResetService (send_code, verify_code, confirm)
- [x] POST /api/v2/ums/auth/password-reset/send (미가입 시 404)
- [x] POST /api/v2/ums/auth/password-reset/verify
- [x] POST /api/v2/ums/auth/password-reset/confirm (email, code, newPassword 재검증)
- [x] 예외 (PasswordResetExpired/Invalid)
- [x] 비밀번호 찾기 테스트 (service, router)
- [x] Swagger 문서화
- [ ] (운영 보강) 코드 해시 저장, 레이트리밋, 시도 횟수 제한
- [ ] (운영 보강) 코드 발송 재발송 최소 간격 제한 (최근 발송 후 N초 이내 429, email-verification·password-reset 공통)

## UMS - Refresh Token (8단계)

- [ ] refresh token 발급 로직 (login 시 access + refresh 쌍 발급)
- [ ] POST /api/v2/ums/auth/refresh (refresh token으로 access token 재발급)
- [ ] refresh token 저장/검증 로직
- [ ] refresh token 테스트
- [ ] Swagger 문서화

## Admin - 유저 관리

- [ ] GET /api/v2/ums/admin/users (유저 목록 조회)
- [ ] GET /api/v2/ums/admin/users/{uid} (유저 상세 조회)
- [ ] PATCH /api/v2/ums/admin/users/{uid} (유저 정보 수정)
- [ ] DELETE /api/v2/ums/admin/users/{uid} (유저 삭제)
- [ ] 어드민 권한 검증 (UserType.ADMIN)
- [ ] 테스트
- [ ] Swagger 문서화

## Admin - 게시글 관리

- [ ] GET /api/v2/admin/posts (게시글 목록 조회)
- [ ] GET /api/v2/admin/posts/{id} (게시글 상세 조회)
- [ ] DELETE /api/v2/admin/posts/{id} (게시글 삭제)
- [ ] 테스트
- [ ] Swagger 문서화
