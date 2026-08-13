# yologram-api-v2

FastAPI 기반 API 서버.

## 기술 스택

- FastAPI, Python 3.12+, uv, uvicorn
- pydantic-settings (APP_PROFILE 분기), pydantic[email] (이메일 검증)
- SQLAlchemy + PyMySQL
- redis-py: Valkey 캐시(닉네임·뉴스 첫 페이지) — api-v1 미러 (동일 키·JSON 호환, 1s 타임아웃, 장애 시 DB 폴백)
- 도메인 경계: app/infra/client/{대상도메인}의 ApiClient — 타 도메인 DB 접근은 이 층에서만 (api-v1 규칙 미러)
- bcrypt (비밀번호 해싱), PyJWT (JWT)
- boto3 (AWS SES)
- OpenTelemetry SDK + OTLP exporter (logs/metrics/traces) + FastAPI·system-metrics 자동 계측
- ApiEnvelop 응답 래퍼, AppException 예외 처리, CORS 전체 허용
- Dockerfile (python:3.12-slim multi-stage)


## API 문서

- Swagger UI: [https://api.yologram.link/api/v2/docs](https://api.yologram.link/api/v2/docs)

## 의존성 설치

```bash
uv sync
```

## 로컬 실행

로컬 캐시는 localhost:16379 Redis (.env의 CACHE_REDIS_HOST/PORT) — 미기동이어도 DB 폴백으로 정상 동작.

기본 프로파일 (default):
```bash
uv run uvicorn app.main:app --reload --port 5002
```

프로파일 지정:
```bash
APP_PROFILE=prod uv run uvicorn app.main:app --reload --port 5002
```

prod DB 연결:
```bash
./scripts/run-prod.sh
```

서버 기본 주소: http://localhost:5002

## 테스트

```bash
uv run pytest tests/ -v
```

- pytest + TestClient, mock(unittest.mock)



## Observability (Grafana Cloud, OTLP)

| 구분 | 라이브러리 |
|---|---|
| 자동 계측 | opentelemetry-instrumentation-fastapi + opentelemetry-instrumentation-system-metrics |
| Logs | opentelemetry-sdk (LoggerProvider + OTLPLogExporter) |
| Traces | opentelemetry-sdk (TracerProvider + OTLPSpanExporter) |
| Metrics | opentelemetry-sdk (MeterProvider + OTLPMetricExporter) |

설정값은 ECS secrets (Parameter Store) 에서 환경변수로 주입.
OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS를 OpenTelemetry SDK가 자동으로 읽음.

