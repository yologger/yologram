# yologram-api-v1

Spring Boot MVC (Kotlin) API 서버.

## Observability (Grafana Cloud, OTLP)

| 구분 | 라이브러리 |
|---|---|
| 자동 계측 | spring-boot-starter-actuator |
| Logs | opentelemetry-logback-appender |
| Traces | micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp |
| Metrics | micrometer-registry-otlp |

설정값은 AWS Parameter Store에서 주입 (`/yologram/service/yologram-api-v1_{profile}/`).

### 프로필별 동작

- local: 콘솔 로그만 출력
- prod: 콘솔 + Grafana Cloud (OTLP) 전송
