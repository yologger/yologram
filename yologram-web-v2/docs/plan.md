# yologram-web-v2 Observability Plan

## Trace 1차 범위

- server-side trace만 우선 적용
- Next.js App Router 요청/렌더링/fetch span 수집
- Grafana Cloud OTLP endpoint로 direct push

## 구현 방향

- `src/instrumentation.ts`에서 Next.js instrumentation register
- Node runtime에서만 OpenTelemetry SDK 초기화
- `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS` 기반 exporter 구성
- endpoint가 없으면 tracing setup 생략
- Docker 빌드는 Yarn Berry non-zero-install 기준으로 유지
- Next.js 배포는 `standalone` 없이 일반 서버 실행으로 유지
- 서버 런타임 env(`APP_ENV`)와 public env(`NEXT_PUBLIC_APP_ENV`)를 분리 관리

## 제외 범위

- browser RUM
- client-side tracing
- logs
- metrics
