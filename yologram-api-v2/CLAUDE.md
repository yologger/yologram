# yologram-api-v2 프로젝트 지침

## 기술 스택

- FastAPI, Python 3.12+
- uv 패키지 매니저
- Pydantic Settings
- SQLAlchemy + PyMySQL (ORM + DB)
- bcrypt (비밀번호 해싱)
- PyJWT (JWT 토큰)
- boto3 (AWS SES 이메일 발송)
- pytest + httpx (테스트)

## 설정 관리

- .env 파일로 로컬 설정
- ECS secrets (Parameter Store)에서 환경변수로 주입
- DB 환경변수: DB_URL, DB_USERNAME, DB_PASSWORD
- JWT 환경변수: JWT_SECRET (Parameter Store에서 주입)
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS는 OpenTelemetry SDK가 자동으로 읽음
- SES 발신 주소: ses_from_address (기본 no-reply@yologram.link)
- AWS 자격증명: prod는 ECS Task Role, 로컬은 AWS_PROFILE 환경변수 (scripts/run-prod.sh에서 export)
- pydantic-settings: 필드명을 대문자 환경변수로 자동 매핑

## DB

- SQLAlchemy + PyMySQL
- R/W splitting 없음 (단일 writer)
- app/config/database.py: engine, SessionLocal, get_db

## API

- 응답 래퍼: ApiEnvelop ({ "data": T })
- 예외: AppException → { "errorMessage", "errorCode" }
- 라우팅 예외도 동일 형식: 404 → NOT_FOUND, 405 → METHOD_NOT_ALLOWED (StarletteHTTPException 핸들러)
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

## 이메일 인증

- EmailSender 프로토콜로 발송 추상화
- StubEmailSender: 로그 출력 (app_profile != prod, 개발/테스트용)
- SesEmailSender: AWS SES 발송 (app_profile == prod, boto3)
- get_email_sender 의존성으로 프로파일에 따라 주입
- 발신 주소: no-reply@yologram.link (ses_from_address 설정)
- 리전: ap-northeast-2
- 자격증명: ECS Task Role (prod), AWS_PROFILE 환경변수 (로컬, scripts/run-prod.sh)
- EmailVerificationCode 모델: email, code(6자리), verified, expired_at(5분), created_at / 테이블 email_verification_codes
- 엔드포인트: POST /api/v2/ums/auth/email-verification/send, /verify
- 회원가입 시 이메일 인증 필수 (UserService.join에서 verified 확인, 가입 후 코드 삭제)

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
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
