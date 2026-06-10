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

## Auth

- JWT 인증은 `Authorization: Bearer {token}` 헤더를 사용합니다.
- `validate-token`은 로그인 직후 토큰 저장값을 안정적으로 확인하기 위해 master DB 트랜잭션으로 조회합니다.

