# yologram-api-v2 프로젝트 지침

## 기술 스택

- FastAPI, Python 3.12+
- uv 패키지 매니저
- Pydantic Settings

## 설정 관리

- .env 파일로 로컬 설정
- ECS secrets (Parameter Store)에서 환경변수로 주입
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS는 OpenTelemetry SDK가 자동으로 읽음
- pydantic-settings: 필드명을 대문자 환경변수로 자동 매핑

## Observability

- Grafana Cloud OTLP direct push
- Logs: OpenTelemetry LoggerProvider + OTLPLogExporter (app/config/logging.py)
- Metrics: OpenTelemetry MeterProvider + OTLPMetricExporter + SystemMetricsInstrumentor (app/config/metrics.py)
- Traces: OpenTelemetry TracerProvider + OTLPSpanExporter + FastAPIInstrumentor (app/config/tracing.py)
- Resource 속성: service.name, deployment.environment.name, service.instance.id, service.namespace

## 포트

- 로컬/ECS 모두 5000

## 배포

- Docker (python:3.12-slim multi-stage)
- ECS Fargate
- GitHub Actions: Docker build → ECR push → ECS 재배포
