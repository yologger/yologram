# yologram-api-v1

Spring Boot MVC (Kotlin) API 서버.

## 기술 스택

- Spring Boot 3.5 (Kotlin), Java 17, Gradle (Kotlin DSL)
- Spring Data JPA + QueryDSL 5.1 (ORM + 복잡 쿼리)
- MySQL (RDS) + Testcontainers (테스트)
- R/W splitting (MasterSlaveRoutingDataSource)
- Spring Security Crypto (BCrypt 비밀번호 해싱)
- Auth0 java-jwt (JWT)
- kotlin-logging (로깅)
- springdoc-openapi (Swagger)
- OpenTelemetry: micrometer-registry-otlp, micrometer-tracing-bridge-otel, opentelemetry-logback-appender
- AWS SDK v2 (SES), Spring Cloud AWS (Parameter Store)

## 사전 준비

- Java 17+
- Gradle

## 로컬 실행

```bash
./gradlew bootRun
```

서버 기본 주소: http://localhost:5001

## 테스트

```bash
./gradlew test
```
