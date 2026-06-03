# yologram-api-v2 에이전트 가이드

## 프로젝트 개요

FastAPI 기반 API 서버. ECS Fargate에서 운영.

## 주요 파일

- app/main.py: 앱 진입점 (logging, metrics, tracing 초기화)
- app/config/settings.py: Pydantic Settings (환경변수 매핑)
- app/config/logging.py: OTLP 로그 설정
- app/config/metrics.py: OTLP 메트릭 설정
- app/config/tracing.py: OTLP 트레이스 설정

## 코드 컨벤션

- 의존성 관리: uv (pyproject.toml + uv.lock)
- 설정: pydantic-settings (환경변수 자동 매핑)
- 로깅: Python logging + OpenTelemetry LoggingHandler

## 빌드/배포

- 빌드: uv sync
- Docker: python:3.12-slim multi-stage
- 배포: GitHub Actions → ECR → ECS
