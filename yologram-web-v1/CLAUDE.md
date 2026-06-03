# yologram-web-v1 프로젝트 지침

## 기술 스택

- React 19, React Router 7, TypeScript
- Vite 빌드
- Ant Design UI + CSS Modules (커스텀 스타일)
- Jotai (상태 관리), axios + TanStack Query (API 통신)
- Yarn Berry (non-zero-install)

## 작업 규칙

- UI 컴포넌트는 Ant Design을 우선 사용하고, 커스텀 필요 시 CSS Modules 사용
- 반응형은 useIsMobile 훅으로 분기 (breakpoint: 768px)
- 페이지별 디렉토리로 관리 (pages/invest/, pages/politics/ 등)
- CSS 파일은 해당 컴포넌트와 같은 디렉토리에 .module.css로 배치
- 글로벌 스타일만 styles/ 디렉토리에 분리

## 라우팅

- / → /invest 리다이렉트
- /invest, /politics, /tech, /notifications, /settings 5개 탭
- ResponsiveLayout이 모바일 탭바 / 데스크탑 사이드바 분기

## 로컬 개발

- 포트: 6001 (yarn start:dev)

## 배포

- S3 + CloudFront
- 빌드 결과물(build/)을 S3에 sync
- index.html은 no-cache, 나머지는 1년 캐시 (Vite 해시 파일명)
