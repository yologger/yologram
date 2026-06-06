# yologram-web-v2 프로젝트 지침

## 기술 스택

- Next.js 16 (App Router), TypeScript
- Yarn Berry (non-zero-install)
- standalone 출력 없이 일반 next start 방식

## 환경변수

- NEXT_PUBLIC_*: 빌드 시 인라인, 클라이언트용
- APP_ENV: 서버 런타임 전용, ECS에서 주입
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS: OpenTelemetry SDK가 자동으로 읽음

## Observability

- OpenTelemetry NodeSDK로 Grafana Cloud OTLP direct push
- Traces: OTLPTraceExporter (Next.js 자동 span)
- Metrics: OTLPMetricExporter + HostMetrics (process CPU/memory)
- Logs: OTLPLogExporter (API Route, dynamic 페이지에서 사용)
- 설정: src/instrumentation.ts → src/instrumentation.node.ts

## 테스트

- vitest + jsdom + @testing-library/react + msw
- 테스트 유틸리티: src/test/ (setup.ts, handlers.ts, server.ts, utils.tsx)
- 테스트 파일은 소스 파일 옆에 배치 (colocation)
- yarn test (단일 실행), yarn test:watch (감시 모드)

## 배포

- Docker (Yarn Berry non-zero-install, next start)
- ECS Fargate
- GitHub Actions: Docker build → ECR push → ECS 재배포
