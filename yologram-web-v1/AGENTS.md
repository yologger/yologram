# yologram-web-v1 프로젝트 지침

## 프로젝트 개요

React 기반 하이브리드 웹앱. 투자/정치/기술 서비스를 반응형(모바일 탭바 + 데스크탑 사이드바)으로 제공.

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- src/Router.tsx: 라우팅 정의
- src/App.tsx: 앱 진입점
- src/components/auth/AuthGate.tsx: 저장된 JWT 검증 후 라우터 렌더링
- src/components/auth/RequireAuth.tsx: 보호 라우트 인증 가드
- src/components/layout/ResponsiveLayout.tsx: 반응형 레이아웃 분기
- src/components/common/SubTabLayout.tsx: 서브탭 공통 레이아웃
- src/components/common/SearchBar.tsx: 섹션 검색바 — 데스크탑 인라인 / 모바일 돋보기→오버레이, Enter 시 /{section}/keywords/{키워드} 이동 (백엔드 미연동)
- src/pages/search/KeywordSearchPage.tsx: 키워드 검색 결과 placeholder 페이지 (섹션 공용, 라우트 3개)
- src/components/common/FilterChips.tsx: 필터 칩 공통 컴포넌트
- src/hooks/useIsMobile.ts: 모바일 판별 훅
- src/hooks/useRequireAuth.ts: 미인증 로그인 유도 공용 훅 — 모달("로그인이 필요해요") → /login 이동(returnTo state) → 로그인 후 원위치 복귀. 하트·댓글(포커스 시점)·글쓰기 진입에서 사용, 미인증 진입점은 disabled 대신 이 훅 사용이 규칙

## 작업 규칙

- UI 컴포넌트는 Ant Design을 우선 사용하고, 커스텀 필요 시 CSS Modules 사용
- 반응형은 useIsMobile 훅으로 분기 (breakpoint: 768px)
- 페이지별 디렉토리로 관리 (pages/invest/, pages/politics/ 등)
- CSS 파일은 해당 컴포넌트와 같은 디렉토리에 .module.css로 배치
- 글로벌 스타일만 styles/ 디렉토리에 분리
- 입력 폼 제출 버튼은 클라이언트 유효성 검증 통과 시에만 활성화 (Ant Form은 useFormSubmittable 훅, 수동 상태 폼은 파생 isValid)

## 라우팅

- / → /tech 리다이렉트
- /invest, /politics, /tech, /notifications, /settings 5개 탭
- ResponsiveLayout이 모바일 탭바 / 데스크탑 사이드바 분기

## 인증

- JWT는 Jotai `authAtom`과 localStorage에 저장
- `AuthGate`가 앱 시작 시 저장된 토큰을 `validate-token`으로 검증한 뒤 라우터 렌더링
- `RequireAuth`는 인증 초기화 완료 후 보호 라우트 진입 여부만 판단

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- vitest + jsdom + @testing-library/react + msw
- 테스트 유틸리티: src/test/ (setup.ts, handlers.ts, server.ts, utils.tsx)
- 테스트 파일은 소스 파일 옆에 배치 (colocation)
- yarn test (단일 실행), yarn test:watch (감시 모드)

## 로컬 개발

- 포트: 3001 (yarn start:dev)
- API URL: http://localhost:5001 (.env.development)

## 배포

- S3 + CloudFront
- 빌드 결과물(build/)을 S3에 sync
- index.html은 no-cache, 나머지는 1년 캐시 (Vite 해시 파일명)
