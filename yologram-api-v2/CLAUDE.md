# yologram-api-v2 프로젝트 지침

## 기술 스택

- FastAPI, Python 3.12+
- uv 패키지 매니저
- Pydantic Settings
- SQLAlchemy + PyMySQL (ORM + DB)
- bcrypt (비밀번호 해싱)
- PyJWT (JWT 토큰)
- pytest + httpx (테스트)

## 설정 관리

- .env 파일로 로컬 설정
- ECS secrets (Parameter Store)에서 환경변수로 주입
- DB 환경변수: DB_URL, DB_USERNAME, DB_PASSWORD
- JWT 환경변수: JWT_SECRET (Parameter Store에서 주입)
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS는 OpenTelemetry SDK가 자동으로 읽음
- pydantic-settings: 필드명을 대문자 환경변수로 자동 매핑

## DB

- SQLAlchemy + PyMySQL
- R/W splitting 없음 (단일 writer)
- app/config/database.py: engine, SessionLocal, get_db

## API

- 응답 래퍼: ApiEnvelop ({ "data": T })
- 예외: AppException → { "errorMessage", "errorCode" }
- CORS: 전체 허용 (*)
- Swagger: /api/v2/docs
- 신규 API 추가 시 Swagger 문서화 필수 (요청/응답 스키마, 에러 코드, 인증 여부)

## 인증

- JWT: PyJWT (HMAC256), api-v1과 동일한 secret/issuer/audience
- 설정: jwt_secret(환경변수), jwt_expire(86400), jwt_issuer(yologram.link), jwt_audience(yologram.client)
- 인증 헤더: Authorization: Bearer {token}
- get_authenticated_user 의존성으로 인증 정보 주입 (FastAPI Depends)
- 토큰 저장: DB User.access_token (로그아웃 시 None 처리)
- validate-token: JWT 검증 + DB access_token 일치 확인

## 테스트

- pytest + TestClient
- mock: unittest.mock (MagicMock, patch)
- uv run pytest tests/ -v

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
