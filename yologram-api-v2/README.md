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

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | /api/v2/test | 기본 응답 |
| GET | /api/v2/test/echo | 클라이언트 요청 정보 반환 |
| GET | /api/v2/test/profile | 활성 프로파일 반환 |
| GET | /api/v2/test/property?key=... | 설정값 조회 |
| POST | /api/v2/ums/user/join | 회원가입 |
| POST | /api/v2/ums/auth/login | 로그인 |
| POST | /api/v2/ums/auth/validate-token | 토큰 검증 |
| POST | /api/v2/ums/auth/logout | 로그아웃 |
| POST | /api/v2/ums/auth/email-verification/send | 이메일 인증 코드 발송 |
| POST | /api/v2/ums/auth/email-verification/verify | 이메일 인증 코드 검증 |
| GET | /api/v2/ums/user/me | 회원정보 조회 |
| PATCH | /api/v2/ums/user/me | 회원정보 수정 |
| PATCH | /api/v2/ums/user/me/password | 비밀번호 변경 |

API 문서: http://localhost:5002/api/v2/docs

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

