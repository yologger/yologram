# yologram-web-v2

Next.js 16 기반 웹 애플리케이션.

## 기술 스택

- Next.js 16 (App Router), TypeScript
- Ant Design (@ant-design/cssinjs + Next.js registry)
- Jotai (상태 관리), axios + TanStack Query (API 통신)
- react-markdown (뉴스 요약 렌더)
- OpenTelemetry NodeSDK (server-side logs/metrics/traces)
- Vitest + Testing Library + MSW (테스트)
- Yarn Berry (non-zero-install), 일반 next start (standalone 미사용)

## 디렉토리 구조

```
src/
├── app/          → Next.js App Router 페이지
├── apis/         → API 통신
├── components/   → 공통 컴포넌트
├── hooks/        → 커스텀 훅
├── queries/      → TanStack Query 훅
├── stores/       → Jotai 상태
├── styles/       → 스타일
├── types/        → 타입 정의
├── lib/          → 유틸리티 (api 클라이언트, 시간 포맷 등)
└── test/         → msw 핸들러·테스트 셋업
```

## 실행

의존성 설치:

```bash
yarn install
```

개발 서버 실행:

```bash
yarn start:dev    # http://localhost:3002
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

## 테스트

- vitest + Testing Library + msw

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
