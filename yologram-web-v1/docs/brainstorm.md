# yologram-web-v1 브레인스토밍

## 목적

- yologram-web-v2(Next.js)와 동일한 기능을 React로 구현
- React vs Next.js 비교 학습용 토이프로젝트

## 기술 스택

- React 19
- React Router 7
- TypeScript
- Vite 8
- Yarn Berry
- Node 24

## 페이지 구성

- / : 메인 페이지
- /test : 테스트 페이지

## 환경 분리

- .env.development : 로컬 개발
- .env.staging : 스테이징
- .env.production : 프로덕션
- Vite 빌트인 모드로 환경 분리 (--mode staging, --mode production)
- 환경 변수 prefix: VITE_ (Next.js의 NEXT_PUBLIC_ 대응)

## Dockerfile

- multi-stage 빌드 (의존성 설치 + 빌드 → nginx로 정적 파일 서빙)
- React는 SPA 빌드 결과물이 정적 파일이므로 Next.js(standalone node)와 다름
- 빌드 시 --mode arg로 환경 지정

## React vs Next.js 비교 포인트

- 라우팅: React Router(코드 기반) vs App Router(파일 기반)
- 환경 변수: VITE_ vs NEXT_PUBLIC_
- 빌드: Vite → 정적 파일 vs Next.js → standalone Node 서버
- 배포: nginx(정적 서빙) vs node server.js
- SSR: React는 CSR only, Next.js는 SSR/SSG 가능

## 미정

- CSS 라이브러리 (yologram-web-v2와 동일하게 추후 결정)
- 상태관리
- API 연동
