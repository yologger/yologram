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

- [ ] JWT 유틸 (생성, 검증)
- [ ] AuthService (login, logout, validateToken)
- [ ] POST /api/v1/ums/auth/login
- [ ] POST /api/v1/ums/auth/logout
- [ ] POST /api/v1/ums/auth/validate-token
- [ ] 로그인/로그아웃 테스트

## UMS - 유저 조회/탈퇴 (3단계)

- [ ] GET /api/v1/ums/user/{uid}
- [ ] DELETE /api/v1/ums/user/withdraw
- [ ] 유저 조회/탈퇴 테스트
