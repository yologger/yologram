# yologram-admin-web 프로젝트 지침

## 프로젝트 개요

React 기반 어드민 웹. 회원/카테고리/게시글/RSS 피드 관리 기능을 반응형(데스크탑 고정 사이드바 + 모바일 토글 Drawer 사이드바)으로 제공 예정 (현재 부트스트랩 — 전 메뉴 준비 중).

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- src/Router.tsx: 라우팅 정의 (전 메뉴 ComingSoon — 기능 구현 시 페이지 컴포넌트로 교체)
- src/App.tsx: 앱 진입점
- src/components/layout/AdminLayout.tsx: 어드민 공통 레이아웃 (반응형 분기 — 데스크탑 고정 사이드바 / 모바일 햄버거 버튼 + Drawer 사이드바)
- src/components/layout/menu.tsx: 메뉴 정의 공용 상수 (사이드바·Drawer 공유)
- src/hooks/useIsMobile.ts: 모바일 판별 훅

## 작업 규칙

- web-v1과 동일 스택·컨벤션 유지 (React 19, antd 6, react-query, jotai, react-router 7)
- 테마 컬러는 파란 계열 (#1677ff, hover #4096ff, active #0958d9) — web-v1 초록(#08979c)·web-v2 핑크(#e7689a)와 서비스별 구분
- UI 컴포넌트는 Ant Design을 우선 사용하고, 커스텀 필요 시 CSS Modules 사용
- 반응형은 useIsMobile 훅으로 분기 (breakpoint: 768px, web-v1 동일 훅). 어드민은 모바일에서 하단 탭바 대신 토글 Drawer 사이드바 사용
- 페이지별 디렉토리로 관리 (pages/users/, pages/posts/ 등 — 기능 구현 시 생성)
- CSS 파일은 해당 컴포넌트와 같은 디렉토리에 .module.css로 배치
- 글로벌 스타일만 styles/ 디렉토리에 분리
- API는 api-v1(Spring)을 호출 (VITE_APP_API_URL)

## 라우팅

- / → /dashboard 리다이렉트
- /dashboard, /users, /categories, /posts, /feeds 5개 메뉴 (전부 준비 중)
- 알 수 없는 경로는 /dashboard로 리다이렉트

## 인증

- 미구현. 어드민 인증 방식(앱 레벨 ADMIN role vs WAF/CloudFront 레벨) 결정 후 구현 예정 (docs/todos.md 참조)

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- vitest + jsdom + @testing-library/react + msw
- 테스트 유틸리티: src/test/ (setup.ts, handlers.ts, server.ts, utils.tsx)
- 테스트 파일은 소스 파일 옆에 배치 (colocation)
- yarn test (단일 실행), yarn test:watch (감시 모드)

## 로컬 개발

- 포트: 3003 (yarn start:dev)
- API URL: http://localhost:5001 (.env.development)

## 배포

- S3 + CloudFront (admin.yologram.link)
- 빌드 결과물(build/)을 S3에 sync
- index.html은 no-cache, 나머지는 1년 캐시 (Vite 해시 파일명)
