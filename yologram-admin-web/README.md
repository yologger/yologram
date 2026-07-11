# yologram-admin-web

yologram 어드민 웹. 회원/카테고리/게시글/RSS 피드 관리 기능을 제공 예정 (현재 부트스트랩 상태 — 전 메뉴 준비 중).

## 기술 스택

- React 19 + React Router 7 (web-v1과 동일 구성)
- Vite + TypeScript
- Ant Design + CSS Modules (테마 컬러: 파란 계열 #1677ff)
- Jotai (상태 관리), axios + TanStack Query (API 통신)
- Yarn Berry (non-zero-install), Node 24
- API 대상: api-v1 (Spring)

## 실행

```bash
yarn install
yarn start:dev    # http://localhost:3003
```

## 빌드

```bash
yarn build:prod
```

## 테스트

- vitest + Testing Library + msw

## 디렉토리 구조

```
src/
├── components/
│   ├── layout/       → 어드민 레이아웃 (데스크탑 전용 사이드바)
│   └── common/       → 공통 UI 컴포넌트 (ComingSoon)
├── styles/           → 글로벌 스타일
├── test/             → 테스트 유틸리티 (setup, msw, renderWithProviders)
└── types/            → 타입 선언
```

## 배포

- S3 + CloudFront (admin.yologram.link)
- 빌드 결과물(build/)을 S3에 sync
- index.html은 no-cache, 나머지는 1년 캐시 (Vite 해시 파일명)
