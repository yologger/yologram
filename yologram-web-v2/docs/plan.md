# yologram-web-v2 Observability Plan

## Trace

- server-side trace 적용
- Next.js App Router 요청/렌더링/fetch span 수집
- Grafana Cloud OTLP endpoint로 direct push

## Metrics

- NodeSDK 직접 구성으로 Grafana Cloud OTLP endpoint에 direct push
- 프로세스 메트릭: @opentelemetry/host-metrics (process.cpu.utilization, process.memory.usage)
- HTTP 메트릭: @opentelemetry/instrumentation-http 추가했으나 Next.js 환경에서 http.server.request.duration 미생성 확인 (한계)
- 요청 수는 http.server.request.duration count 기반이 목표였으나, 현재는 trace 기반으로 확인

## 구현 방향

- `src/instrumentation.ts`에서 Next.js instrumentation register
- Node runtime에서만 OpenTelemetry SDK 초기화
- `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS` 기반 exporter 구성 (trace, metrics 공유)
- endpoint가 없으면 trace/metrics setup 생략
- Docker 빌드는 Yarn Berry non-zero-install 기준으로 유지
- Next.js 배포는 `standalone` 없이 일반 서버 실행으로 유지
- 서버 런타임 env(`APP_ENV`)와 public env(`NEXT_PUBLIC_APP_ENV`)를 분리 관리
- production 장기 권장안은 Alloy지만 현재는 direct push

## 제외 범위

- browser RUM
- client-side tracing
- logs
- custom metrics (이번 단계에서 추가하지 않음)

## 향후 개선 선택지

- Grafana Alloy + spanmetrics로 trace에서 request 메트릭 추출
- access log 보강
- 최소 custom metric 추가
