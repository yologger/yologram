# yologram-web-v1

React 기반 하이브리드 웹 애플리케이션. 투자/정치/기술 서비스를 반응형으로 제공.

## 기술 스택

- React 19 + React Router 7
- Vite + TypeScript
- Ant Design + CSS Modules
- Jotai (상태 관리), axios + TanStack Query (API 통신)
- Yarn Berry (non-zero-install)

## 실행

```bash
yarn install
yarn start:dev    # http://localhost:3001
```

## 빌드

```bash
yarn build:prod
```

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

## 반응형 구조

- 모바일 (< 768px): 하단 탭바 (투자, 정치, 기술, 알림, 설정)
- 데스크탑 (>= 768px): 왼쪽 사이드바 (toggle 가능)

## 인증

- JWT는 `authAtom`에 저장하며 localStorage로 유지합니다.
- `AuthGate`가 앱 시작 시 저장된 토큰을 `validate-token`으로 확인한 뒤 라우터를 렌더링합니다.

## 로컬 포트

- 개발 서버: 3001

## 배포

- S3 + CloudFront (정적 파일 호스팅)
- GitHub Actions: yarn build → S3 sync
