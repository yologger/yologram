# yologram-api-v1

Spring Boot MVC (Kotlin) API 서버.

## 기술 스택

- Spring Boot 3.5 (Kotlin), Java 17, Gradle (Kotlin DSL)
- Spring Web MVC + Actuator (헬스체크) + Bean Validation
- Spring Data JPA + QueryDSL 5.1 (ORM + 복잡 쿼리)
- MySQL (RDS) + Testcontainers (테스트)
- R/W splitting (MasterSlaveRoutingDataSource)
- Redis(Valkey) 캐시: Spring Data Redis + Lettuce 수동 빈 (cache.data.redis.* 커스텀 프로퍼티, 자동구성 exclude) — 닉네임·뉴스 첫 페이지 cache-aside, 장애 시 DB 폴백 (1s 타임아웃 + REJECT_COMMANDS)
- 도메인 경계: infra/client/{대상도메인}의 {대상도메인}ApiClient — 타 도메인 DB 접근은 이 층에서만 (MSA 분리 시 Rest 구현으로 교체)
- Spring Security Crypto (BCrypt 비밀번호 해싱)
- Auth0 java-jwt (JWT)
- kotlin-logging (로깅)
- springdoc-openapi (Swagger)
- OpenSearch Java Client 2.25.0 + httpclient5 (게시글 검색 — nori 형태소, alias 참조)
- OpenTelemetry: micrometer-registry-otlp, micrometer-tracing-bridge-otel, opentelemetry-logback-appender
- AWS SDK v2 (SES), Spring Cloud AWS (Parameter Store)

## API 문서

- Swagger UI: [https://api.yologram.link/api/v1/docs](https://api.yologram.link/api/v1/docs)