# yologram-web-v1

React 기반 하이브리드 웹 애플리케이션. 투자/정치/기술 서비스를 반응형으로 제공.

## 기술 스택

- React 19 + React Router 7
- Vite + TypeScript
- Ant Design + CSS Modules
- Jotai (상태 관리), axios + TanStack Query (API 통신)
- Yarn Berry (non-zero-install), Node 24

## React ↔ Next.js 비교 (학습 목적)

web-v2(Next.js)와 동일 기능을 React로 구현한 비교 학습용 프로젝트.

- 라우팅: React Router(코드 기반) vs App Router(파일 기반)
- 환경변수: `VITE_` prefix vs `NEXT_PUBLIC_`
- 빌드: Vite 정적 파일 vs Next.js node 서버
- 배포: S3 + CloudFront vs node server
- SSR: React는 CSR only, Next.js는 SSR/SSG 가능

## 실행

```bash
yarn install
yarn start:dev    # http://localhost:3001
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
│   ├── layout/       → 반응형 레이아웃 (사이드바, 탭바)
│   └── common/       → 공통 UI 컴포넌트 (SubTabLayout, FilterChips)
├── pages/
│   ├── invest/       → 투자 (뉴스, 관심 뉴스, 커뮤니티, 정보)
│   ├── politics/     → 정치 (뉴스, 관심 뉴스, 커뮤니티, 정보)
│   ├── tech/         → 기술 (뉴스, 관심 뉴스)
│   ├── notifications/→ 알림
│   └── settings/     → 설정
├── hooks/            → 커스텀 훅
├── styles/           → 글로벌 스타일
└── types/            → 타입 선언
```

