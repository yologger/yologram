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

## UMS - 유저 조회/탈퇴 (3단계)

- [ ] GET /api/v2/ums/user/{uid}
- [ ] DELETE /api/v2/ums/user/withdraw
- [ ] 유저 조회/탈퇴 테스트
