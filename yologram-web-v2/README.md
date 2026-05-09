# yologram-web-v2

Next.js 16 기반 웹 애플리케이션.

## 실행

의존성 설치:

```bash
yarn install
```

개발 서버 실행:

```bash
yarn dev
```

빌드:

```bash
yarn build:staging
yarn build:prod
```

로컬 프로덕션 실행:

```bash
APP_ENV=production yarn start
```

## Docker

- Yarn Berry는 사용하지만 zero-install은 사용하지 않는다.
- 컨테이너 빌드 시 이미지 내부에서 `yarn install --immutable`로 의존성을 설치한다.
- Next.js는 `standalone` 출력 없이 일반 `next start` 방식으로 실행한다.
- `NEXT_PUBLIC_APP_ENV`는 빌드/클라이언트용 값이고, `APP_ENV`는 서버 런타임에서만 주입한다.
- Docker runtime은 Yarn 4를 이미지에 준비한 뒤 `yarn start`로 실행한다.

## Observability (Grafana Cloud, OTLP direct push)

Trace와 Metrics는 OpenTelemetry NodeSDK로 Grafana Cloud OTLP endpoint에 direct push.

| 구분 | 라이브러리 | 비고 |
|---|---|---|
| Traces | @opentelemetry/sdk-node + exporter-trace-otlp-http | Next.js 자동 span 수집 |
| Metrics (프로세스) | @opentelemetry/host-metrics | process.cpu.utilization, process.memory.usage |
| Metrics (HTTP) | @opentelemetry/instrumentation-http | Next.js 환경에서 http.server.request.duration 미생성 (한계) |
| Logs | - | 미적용. stdout 출력만 사용 |

설정값은 런타임 환경변수로 주입.
`OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS`는 trace와 metrics가 같은 gateway를 공유.

### 프로필별 동작

- development/local: OTLP endpoint가 없으면 trace/metrics 비활성
- staging/production: OTLP endpoint가 있으면 Grafana Cloud로 전송

### 기본 리소스 속성

- `service.name`: `yologram-web-v2`
- `service.namespace`: `yologram`
- `deployment.environment.name`: `APP_ENV`를 우선 사용하고 없으면 `NEXT_PUBLIC_APP_ENV`

### 요청 수 확인 방법

- http.server.request.duration 메트릭은 Next.js 환경에서 생성되지 않음
- 요청 수는 Grafana Tempo에서 trace 기반으로 확인

### 향후 개선 선택지

- Grafana Alloy + spanmetrics로 trace에서 request 메트릭 추출
- access log 보강
- 최소 custom metric 추가
- production 장기 권장안은 Alloy지만 현재는 direct push
