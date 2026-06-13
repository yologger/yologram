# yologram-api-v1 프로젝트 지침

## 기술 스택

- Spring Boot 3.5 (Kotlin), Java 17
- Gradle (Kotlin DSL)
- Spring Data JPA + QueryDSL (ORM + 복잡 쿼리)
- MySQL (RDS) + Testcontainers (테스트)
- R/W splitting (MasterSlaveRoutingDataSource)
- BCryptPasswordEncoder (비밀번호 해싱)
- Auth0 java-jwt (JWT 토큰)
- kotlin-logging (로깅)
- springdoc-openapi (Swagger)
- AWS SDK v2 (SES 이메일 발송)

## 설정 관리

- application.yaml: 공통 설정 (OTLP endpoint placeholder)
- application-local.yaml: 로컬 개발 (AWS Parameter Store)
- application-prod.yaml: 프로덕션 (AWS Parameter Store, instance-profile)
- 설정값은 AWS Parameter Store에서 주입 (/yologram/service/yologram-api-v1_{ENV}/)

## Observability

- Grafana Cloud OTLP direct push
- Logs: opentelemetry-logback-appender + OpenTelemetryLoggingConfig
- Metrics: micrometer-registry-otlp
- Traces: micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp
- Resource 속성: service.name, deployment.environment.name, service.instance.id, service.namespace

## 인증

- JWT: Auth0 java-jwt (HMAC256)
- 설정: yologram.auth.jwt.secret/expire/issuer/audience (Parameter Store + application.yaml)
- 인증 헤더: Authorization: Bearer {token}
- @AuthenticatedUser + AuthenticatedUserResolver로 인증 정보 주입
- 토큰 저장: DB User.accessToken (로그아웃 시 null 처리)
- validate-token: JWT 검증 + DB accessToken 일치 확인
- validate-token은 로그인 직후 replica lag를 피하기 위해 master DB 트랜잭션으로 조회

## 이메일 인증

- EmailSender 인터페이스로 발송 추상화
- @Profile("!prod") StubEmailSender: 로그 출력 (개발/테스트용)
- @Profile("prod") SesEmailSender: AWS SES 발송 (SesConfig에서 SesClient 빈 수동 등록)
- 발신 주소: no-reply@yologram.link (IAM 정책으로 한정, 변경 시 인프라 수정 필요)
- 리전: ap-northeast-2 (SES 도메인 인증 리전과 동일)
- 자격증명: ECS Task Role (prod), AWS_PROFILE 환경변수 (로컬)
- EmailVerificationCode 엔티티: email, code(6자리), verified, expiredAt(5분)
- 회원가입 시 이메일 인증 필수 (UserService.join에서 verified 확인)

## 비밀번호 찾기

- 방식: 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (이메일 인증과 동일 패턴/SES 재사용)
- 저장: 별도 테이블 password_reset_codes (PasswordResetCode 엔티티: email, code, verified, expiredAt 5분, createdAt)
- PasswordResetService: sendCode(미가입 시 UserNotFoundException 404, 기존 코드 삭제 후 발송), verifyCode(verified=true), confirm(email·code·newPassword 재검증 후 변경·코드 삭제)
- 엔드포인트: POST /api/v1/ums/auth/password-reset/send·verify·confirm
- 예외: PasswordResetExpiredException/PasswordResetInvalidException (400)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- Testcontainers (MySQL) 통합 테스트
- Mockito + MockMvc 슬라이스 테스트

## Swagger

- Swagger UI: /api/v1/docs
- api-docs: /api/v1/api-docs

## CORS

- WebConfig에서 전체 허용 (*) — 인증 구현 시 origin 제한 필요

## 포트

- 기본(ECS): 5000
- 로컬: 5001 (application-local.yaml에서 override)

## 배포

- Docker (amazoncorretto:17-alpine, jar copy only)
- ECS Fargate
- GitHub Actions: Gradle build (--build-cache) → ECR push → ECS 재배포
