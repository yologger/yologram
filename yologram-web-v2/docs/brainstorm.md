# yologram-web-v2 브레인스토밍

## 기술 스택 (레거시 yologram-web-v2 기준 + Next.js 전환)

- Next.js (App Router) - 레거시는 React + Vite, Next.js로 전환
- TypeScript
- Ant Design - 레거시와 동일
- Emotion - 레거시와 동일
- TanStack Query - 레거시와 동일
- Jotai - 레거시와 동일
- axios - 레거시와 동일
- Docker (standalone 모드)

## 패키지 매니저

- yarn - 레거시와 동일

## 페이지 구성

- / : 메인 페이지
- /test : 테스트 페이지

## 환경 분리

- .env.development : 로컬 개발
- .env.staging : 스테이징
- .env.production : 프로덕션
- 환경 변수 prefix: NEXT_PUBLIC_ (레거시는 VITE_)
- 주요 변수: NEXT_PUBLIC_APP_ENV, API URL, AUTH TOKEN KEY
- 서버 런타임 env(`APP_ENV`)는 `.env`가 아니라 실행 주체(package script, ECS)가 주입

## src/ 디렉토리 구조 (레거시 참고)

- app/ : Next.js App Router 페이지 (레거시 pages/ 대응)
- apis/ : API 통신
- components/ : 공통 컴포넌트
- hooks/ : 커스텀 훅
- queries/ : TanStack Query 훅
- stores/ : Jotai 상태
- styles/ : 스타일
- types/ : 타입 정의
- utils/ : 유틸리티

## Dockerfile

- multi-stage 빌드 (의존성 설치 + 빌드 → standalone 실행)
- Yarn Berry는 사용하되 zero-install은 사용하지 않음
- 빌드 컨테이너 내부에서 `yarn install --immutable`
- Next.js는 일반 `next start` 방식으로 실행
- 빌드 시 ENV arg로 환경 지정
- 포트 3000

## Observability

- 1차는 server-side trace만 적용
- Next.js `src/instrumentation.ts`에서 런타임 부팅 시 OTel 등록
- Node runtime에서만 tracing SDK 초기화
- Grafana Cloud OTLP endpoint로 direct push
- 운영 env는 `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS`, `OTEL_SERVICE_NAME` 사용
- 환경명은 `APP_ENV`를 우선 사용하고 없으면 `NEXT_PUBLIC_APP_ENV` fallback

## React → Next.js 전환 시 차이점

- 라우팅: React Router → App Router (파일 기반)
- 환경 변수 prefix: VITE_ → NEXT_PUBLIC_
- 빌드: Vite → Next.js 빌드
- SSR/SSG 활용 가능
