# yologram-admin-web 프로젝트 지침

## 프로젝트 개요

React 기반 어드민 웹. 유저/카테고리/게시글/뉴스 관리 기능을 반응형(데스크탑 고정 사이드바 + 모바일 토글 Drawer 사이드바)으로 제공 예정 (어드민 로그인·인증 가드 구현, 관리 화면은 준비 중).

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- src/Router.tsx: 라우팅 정의 (전 메뉴 ComingSoon — 기능 구현 시 페이지 컴포넌트로 교체)
- src/App.tsx: 앱 진입점
- src/components/layout/AdminLayout.tsx: 어드민 공통 레이아웃 — 데스크탑: 상단 Header(로고+최상위 Menu horizontal+Avatar·Dropdown 로그아웃) + Sider(현재 섹션의 하위만 Menu inline) / 모바일: 햄버거 + Drawer(2단 그룹 메뉴, footer 로그아웃)
- src/components/layout/menu.tsx: MENU_SECTIONS 2단 메뉴 상수(최상위 섹션 → 하위 경로) + findSelectedSection/findSelectedChildKey (상단·사이드바·Drawer 공유)
- src/hooks/useIsMobile.ts: 모바일 판별 훅
- src/lib/api.ts: axios 인스턴스 + Bearer 인터셉터 (401 전역 인증 초기화 — /ums/admin/auth/ 경로 제외)
- src/stores/auth.ts: authAtom(jotai atomWithStorage — uid/email/name/accessToken)
- src/apis/auth.ts: login/validateToken/logout/createAdminUser (api-v1 /ums/admin/*)
- src/components/auth/: AuthGate(시작 시 저장 토큰 검증), RequireAuth(미인증 /login 리다이렉트)
- src/pages/auth/LoginPage.tsx: 로그인 (어드민은 회원가입·비밀번호찾기 없음)

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

- /login만 비보호, 나머지 전체는 RequireAuth > AdminLayout 하위 보호
- 2단 내비게이션(번개장터 어드민 문법): 상단 바 = 최상위 분류, 사이드바 = 현재 최상위의 하위 분류. 서브탭 없음
- 최상위/하위: 대시보드→[대시보드 /dashboard], 유저 관리→[유저 관리 /ums/users, 어드민 관리 /ums/admin-users], 카테고리 관리→[/categories], 게시글 관리→[/posts], 뉴스 관리→[기술 /news/tech, 투자 /news/invest, 정치 /news/politics]
- 리다이렉트: / → /dashboard, /ums → /ums/users, /news → /news/tech, 미매칭 → /dashboard. 목록 화면들은 아직 placeholder
- 유저 관련 용어는 '회원'이 아닌 '유저' 사용

## 인증

- 어드민 전용 JWT (api-v1 /ums/admin/auth/login·validate-token·logout — 유저 토큰과 분리)
- authAtom(localStorage) + AuthGate(앱 시작 시 validate-token 검증) + RequireAuth(전 메뉴 인증 필수) — web-v1 미러
- 로그아웃은 AdminLayout(사이드바 하단/Drawer footer)에서 modal.confirm 후 API 호출, 성공/실패 무관 토큰 폐기

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- vitest + jsdom + @testing-library/react + msw
- 테스트 유틸리티: src/test/ (setup.ts, handlers.ts, server.ts, utils.tsx)
- 테스트 파일은 소스 파일 옆에 배치 (colocation)
- setup.ts에 ResizeObserver 스텁 (antd 6 Tabs가 요구, jsdom 미제공)
- yarn test (단일 실행), yarn test:watch (감시 모드)

## 로컬 개발

- 포트: 3003 (yarn start:dev)
- API URL: http://localhost:5001 (.env.development)

## 배포

- S3 + CloudFront (admin.yologram.link)
- 빌드 결과물(build/)을 S3에 sync
- index.html은 no-cache, 나머지는 1년 캐시 (Vite 해시 파일명)
