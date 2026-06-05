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
