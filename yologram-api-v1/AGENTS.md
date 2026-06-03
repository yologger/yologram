# yologram-api-v1 에이전트 가이드

## 프로젝트 개요

Spring Boot MVC (Kotlin) API 서버. ECS Fargate에서 운영.

## 주요 파일

- src/main/resources/application.yaml: 공통 설정 (OTLP endpoint, resource attributes)
- src/main/resources/logback-spring.xml: 로깅 설정 (콘솔 + OTEL appender)
- src/main/kotlin/.../config/OpenTelemetryLoggingConfig.kt: OTEL logback 초기화

## 코드 컨벤션

- Kotlin 코드 스타일
- 로깅: kotlin-logging-jvm (io.github.oshai)
- 설정값: AWS Parameter Store에서 Spring property로 주입

## 빌드/배포

- 빌드: ./gradlew build
- Docker: amazoncorretto:17 multi-stage
- 배포: GitHub Actions → ECR → ECS
