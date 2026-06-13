# yologram-web-v2 에이전트 가이드

## 프로젝트 개요

Next.js 16 기반 웹 프론트엔드. ECS Fargate에서 운영.

## 주요 파일

- src/instrumentation.ts: Next.js instrumentation 진입점
- src/instrumentation.node.ts: OpenTelemetry NodeSDK 초기화 (traces, metrics, logs)
- src/lib/logger.ts: 서버사이드 로그 유틸 (logInfo, logError)
- src/app/api/test/route.ts: 테스트 API Route

## 코드 컨벤션

- 서버 런타임 env는 APP_ENV, 클라이언트는 NEXT_PUBLIC_APP_ENV
- 로그는 API Route나 dynamic 페이지에서만 사용 (static 페이지에서는 불가)
- @vercel/otel 사용하지 않음. NodeSDK 직접 구성.
- 입력 폼 제출 버튼은 클라이언트 유효성 검증 통과 시에만 활성화 (Ant Form은 useFormSubmittable 훅, 수동 상태 폼은 파생 isValid)

## 빌드/배포

- 빌드: yarn build:prod (env-cmd로 .env.production 로드)
- Docker: node:24 builder + node:24-alpine runner
- 배포: GitHub Actions → ECR → ECS
