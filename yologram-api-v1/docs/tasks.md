## 인프라

- [ ] ECS 헬스체크 설정: actuator 의존성 추가 + Task Definition에 healthCheck 설정
- [x] GitHub Actions job timeout 설정

## Observability

- [x] Grafana Cloud 연동 - Logs: OTLP (opentelemetry-logback-appender)
- [x] Grafana Cloud 연동 - Traces: OTLP (micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp)
- [x] Grafana Cloud 연동 - Metrics: OTLP (micrometer-registry-otlp)

## DB 설정

- [ ] 의존성 추가 (JPA, MySQL, QueryDSL, Testcontainers)
- [ ] R/W splitting 구성 (MasterSlaveRoutingDataSource)
- [ ] application.yaml JPA/Hibernate 설정
- [ ] application-local.yaml 로컬 DB 설정
- [ ] application-prod.yaml RDS 설정 (Parameter Store)

## UMS - 회원가입 (1단계)

- [ ] User 엔티티 (email, name, nickname, password, avatar, accessToken, type, status)
- [ ] UserType enum (DEFAULT, POLITICIAN, ECONOMIST, ADMIN)
- [ ] UserStatus enum (ACTIVE, INACTIVE, DELETED)
- [ ] UserRepository
- [ ] BCryptPasswordEncoder 설정
- [ ] UserService.join()
- [ ] POST /api/v1/ums/user/join 컨트롤러
- [ ] UserDuplicateException 예외 처리
- [ ] 회원가입 단위 테스트
- [ ] 회원가입 통합 테스트 (Testcontainers)

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
