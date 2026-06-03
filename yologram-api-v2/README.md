# yologram-api-v2

FastAPI 기반 API 서버.

## 사전 준비

- Python 3.12+
- uv (https://docs.astral.sh/uv/)

## 의존성 설치

```bash
uv sync
```

## 로컬 실행

기본 프로파일 (default):
```bash
uv run uvicorn app.main:app --reload --port 5000
```

프로파일 지정:
```bash
APP_PROFILE=prod uv run uvicorn app.main:app --reload --port 5000
```

서버 기본 주소: http://localhost:5000

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | /api/v2/test | 기본 응답 |
| GET | /api/v2/test/echo | 클라이언트 요청 정보 반환 |
| GET | /api/v2/test/profile | 활성 프로파일 반환 |
| GET | /api/v2/test/property?key=... | 설정값 조회 |

API 문서: http://localhost:5000/docs

## Observability (Grafana Cloud, OTLP)

| 구분 | 라이브러리 |
|---|---|
| 자동 계측 | opentelemetry-instrumentation-fastapi + opentelemetry-instrumentation-system-metrics |
| Logs | opentelemetry-sdk (LoggerProvider + OTLPLogExporter) |
| Traces | opentelemetry-sdk (TracerProvider + OTLPSpanExporter) |
| Metrics | opentelemetry-sdk (MeterProvider + OTLPMetricExporter) |

설정값은 ECS secrets (Parameter Store) 에서 환경변수로 주입.
OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS를 OpenTelemetry SDK가 자동으로 읽음.

### 프로필별 동작

- default/local: 콘솔 로그만 출력
- prod: 콘솔 + Grafana Cloud (OTLP) 전송
