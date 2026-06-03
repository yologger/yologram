# yologram-web-v1 에이전트 가이드

## 프로젝트 개요

React 기반 하이브리드 웹앱. 투자/정치 서비스를 반응형(모바일 탭바 + 데스크탑 사이드바)으로 제공.

## 주요 파일

- src/Router.tsx: 라우팅 정의
- src/App.tsx: 앱 진입점
- src/components/layout/ResponsiveLayout.tsx: 반응형 레이아웃 분기
- src/hooks/useIsMobile.ts: 모바일 판별 훅

## 코드 컨벤션

- UI 프레임워크: Ant Design
- 스타일링: CSS Modules (.module.css)
- 상태 관리: (추후 Jotai 예정)
- API 통신: (추후 axios + TanStack Query 예정)

## 빌드/배포

- 빌드: yarn build (Vite)
- 배포: S3 + CloudFront (GitHub Actions)
- 환경 분리: .env.development, .env.staging, .env.production
