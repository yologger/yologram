# yologram-web-v1 구현 기능 및 설계 근거

구현 완료된 화면/기능과 그 설계·UX 근거를 기록한다. (앞으로 할 일은 tasks.md)

React 프론트(CSR) 관점에서 화면·컴포넌트·상태관리·UX를 다룬다.

## 프로젝트 목적

- yologram-web-v2(Next.js)와 동일한 기능을 React로 구현
- React vs Next.js 비교 학습용 토이프로젝트

## 기술 스택

- React 19, React Router 7, TypeScript
- Vite 빌드, Yarn Berry(non-zero-install), Node 24
- Ant Design UI + CSS Modules(커스텀 스타일)
- Jotai(상태 관리), axios + TanStack Query(API 통신)

## 구현된 기능

### 레이아웃/네비게이션
- 반응형 레이아웃: 모바일 탭바 + 데스크탑 사이드바 (ResponsiveLayout, useIsMobile 768px 분기)
- 5개 최상위 탭: /invest, /politics, /tech, /notifications, /settings (/ → /invest 리다이렉트)
- 최상위 탭 순서 기술 우선
- 기술 페이지 서브탭: 커뮤니티·채용 (SubTabLayout)
- 데스크탑 본문 760px 중앙 고정

### 인증 (UMS 연동)
- 회원가입: JoinPage 단계적 폼(이메일 인증 → 이름·닉네임·비밀번호) → POST /api/v1/ums/user/join
- 로그인/로그아웃: 실제 API 호출 (POST /auth/login·logout)
- 토큰 검증: validateToken() (POST /auth/validate-token)
- 이메일 인증: 코드 발송/검증 (POST /auth/email-verification/send·verify)
- 비밀번호 찾기: ForgotPasswordPage 단계적 폼(이메일 → 코드 발송/검증 → 새 비밀번호), apis/auth.ts send/verify/confirm + 뮤테이션 훅 3개
- AuthGate: 앱 시작 시 저장 토큰 검증 후 라우터 렌더링, authAtom localStorage rehydrate

### 설정
- 회원정보 조회: GET /api/v1/ums/user/me (useUserQuery), 설정 페이지 아바타 하단 닉네임 표시
- 회원정보 수정: EditProfilePage(이메일·이름 읽기전용, 닉네임 변경) → PATCH /user/me (useUpdateProfileMutation), 성공 시 설정 이동 + user 쿼리 무효화
- 비밀번호 변경: 현재/새/확인 입력 → PATCH /user/me/password (useChangePasswordMutation)
- 회원탈퇴: 확인 모달 → DELETE /user/me (useWithdrawMutation), 성공 시 localStorage('auth') 제거 + /login 이동
- 활동 - 내가 쓴 글: /settings/my-posts (게시판 필터 기술/투자/정치, 현재 더미)

### 기술 커뮤니티 피드 (백엔드 연동)
- 목록 API GET /api/v1/pms/{section}/posts cursor 무한스크롤(useInfiniteQuery, nextCursor 기준)
- categoryId 서버 필터
- PostCard를 PostSummary 기반·상대시간(lib/date.formatRelativeTime)으로 전환
- 작성 후 invalidate, 피드 더미 atom 제거(내 글 더미만 유지)

### 공통/상태관리
- 전역 상태 관리 Jotai(authAtom)
- 입력 폼 유효성 통과 시 제출 버튼 활성화 (로그인/회원가입/비밀번호 변경/회원정보 수정)

### 개발 환경
- 테스트 환경: vitest + @testing-library/react + msw
- 테스트 유틸리티: src/test/ (setup.ts, handlers.ts, server.ts, utils.tsx)
- 작성된 테스트: JoinPage, LoginPage, EditProfilePage, ForgotPasswordPage, auth.test.ts (로그인/로그아웃/이메일 인증/비밀번호 찾기)

## 설계 근거

### React vs Next.js 비교 포인트 (학습 목적)
- 라우팅: React Router(코드 기반) vs App Router(파일 기반)
- 환경 변수: VITE_ prefix vs NEXT_PUBLIC_
- 빌드: Vite → 정적 파일 vs Next.js → standalone Node 서버
- 배포: nginx/정적 서빙(S3+CloudFront) vs node server.js
- SSR: React는 CSR only, Next.js는 SSR/SSG 가능

### 환경 분리
- .env.development(로컬), .env.staging(스테이징), .env.production(프로덕션)
- Vite 빌트인 모드로 분리(--mode staging, --mode production)
- 환경 변수 prefix VITE_ (Next.js NEXT_PUBLIC_ 대응)
- 로컬 포트 3001, 로컬 API URL http://localhost:5001

### Dockerfile/배포
- multi-stage 빌드(의존성 설치 + 빌드 → nginx로 정적 파일 서빙)
- React SPA 빌드 결과물은 정적 파일이라 Next.js(standalone node)와 다름
- 빌드 시 --mode arg로 환경 지정
- 배포: S3 + CloudFront, build/ 결과물 S3 sync, index.html no-cache·나머지 1년 캐시(Vite 해시 파일명)

### 인증 상태/게이팅
- JWT는 Jotai authAtom + localStorage 저장, AuthState에 name 필드 포함(API 응답)
- AuthGate가 앱 시작 시 저장 토큰을 validate-token으로 검증 후 라우터 렌더링
- RequireAuth는 인증 초기화 완료 후 보호 라우트 진입 여부만 판단
- 401 인터셉터에서 authAtom 초기화(토큰 만료/무효 시 자동 로그아웃)
- 새 탭/새로고침 시 authAtom rehydrate 보강

### 프론트 유효성/UX
- 프론트 validation은 서버와 동일(이메일 형식, 이름/닉네임 2~20자, 비밀번호 8~20자)
- 이메일 인증 완료 전 회원가입 버튼 비활성(EMAIL_NOT_VERIFIED 사전 차단), 이메일 변경 시 인증 상태 초기화·재발송 지원
- 비밀번호 찾기 폼 유효성 게이팅(코드 발송/인증/변경 버튼), 이메일 변경 시 단계 초기화
- 입력 폼 제출 버튼은 클라이언트 유효성 통과 시에만 활성화(Ant Form은 useFormSubmittable 훅, 수동 상태 폼은 파생 isValid)

### 회원탈퇴 UX
- 백엔드가 개발 단계 하드 삭제라 탈퇴 후 같은 이메일 재가입 가능

### 피드 연동 설계
- cursor 무한스크롤은 백엔드 id-only 커서 방식과 일치(nextCursor 유무로만 다음 페이지 판단)
- PostCard는 PostSummary 기반, 상대시간 표시(lib/date.formatRelativeTime)

### 페이지 구성 규칙
- 페이지별 디렉토리 관리(pages/invest/, pages/politics/ 등)
- CSS는 컴포넌트와 같은 디렉토리에 .module.css 배치, 글로벌 스타일만 styles/ 분리
- UI는 Ant Design 우선, 커스텀 필요 시 CSS Modules
