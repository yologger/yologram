# yologram-api-v1 프로젝트 지침

## 기술 스택

- Spring Boot 3.5 (Kotlin), Java 17
- Gradle (Kotlin DSL)

## 설정 관리

- application.yaml: 공통 설정 (OTLP endpoint placeholder)
- application-local.yaml: 로컬 개발 (AWS Parameter Store, yologram 프로필)
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

## Swagger

- Swagger UI: /api/v1/docs
- api-docs: /api/v1/api-docs
- 신규 API 추가 시 Swagger 문서화 필수 (요청/응답 스키마, 에러 코드, 인증 여부)

## CORS

- WebConfig에서 전체 허용 (*) — 인증 구현 시 origin 제한 필요

## 포트

- 기본(ECS): 5000
- 로컬: 5001 (application-local.yaml에서 override)

## 배포

- Docker (amazoncorretto:17-alpine, jar copy only)
- ECS Fargate
- GitHub Actions: Gradle build (--build-cache) → ECR push → ECS 재배포
