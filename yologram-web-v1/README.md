# yologram-web-v1

React 기반 하이브리드 웹 애플리케이션. 투자/정치 서비스를 반응형으로 제공.

## 기술 스택

- React 19 + React Router 7
- Vite + TypeScript
- Ant Design + CSS Modules
- Yarn Berry (non-zero-install)

## 실행

```bash
yarn install
yarn dev
```

## 빌드

```bash
yarn build
```

## 디렉토리 구조

```
src/
├── components/
│   ├── layout/       → 반응형 레이아웃 (사이드바, 탭바)
│   └── common/       → 공통 UI 컴포넌트
├── pages/
│   ├── invest/       → 투자
│   ├── politics/     → 정치
│   └── settings/     → 설정
├── hooks/            → 커스텀 훅
├── styles/           → 글로벌 스타일
└── types/            → 타입 선언
```

## 반응형 구조

- 모바일 (< 768px): 하단 탭바 (투자, 정치, 설정)
- 데스크탑 (>= 768px): 왼쪽 사이드바 (toggle 가능)

## 배포

- S3 + CloudFront (정적 파일 호스팅)
- GitHub Actions: yarn build → S3 sync
