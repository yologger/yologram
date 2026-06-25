# yologram-web-v2 구현 기능 및 설계 근거

구현 완료된 기능과 그 설계/UX 근거를 기록한다. (앞으로 할 일은 tasks.md)

Next.js(App Router) 프론트 관점 — 화면/라우팅/서버·클라이언트 컴포넌트/상태관리/UX 위주.

## 기술 스택

- Next.js (App Router) — 레거시(web-v1)는 React + Vite, Next.js로 전환
- TypeScript
- Ant Design — 레거시와 동일
- Emotion — 레거시와 동일
- TanStack Query — 레거시와 동일
- Jotai — 레거시와 동일
- axios — 레거시와 동일
- Docker (Yarn Berry non-zero-install, 일반 next start 방식)
- 패키지 매니저: yarn (레거시와 동일)

### React → Next.js 전환 시 차이점
- 라우팅: React Router → App Router (파일 기반)
- 환경 변수 prefix: VITE_ → NEXT_PUBLIC_
- 빌드: Vite → Next.js 빌드
- SSR/SSG 활용 가능

### src/ 디렉토리 구조 (레거시 참고)
- app/ : Next.js App Router 페이지 (레거시 pages/ 대응)
- apis/ : API 통신
- components/ : 공통 컴포넌트
- hooks/ : 커스텀 훅
- queries/ : TanStack Query 훅
- stores/ : Jotai 상태
- styles/ : 스타일
- types/ : 타입 정의
- utils/ : 유틸리티

## 구현된 기능

### 인증 (로그인/로그아웃/토큰검증)
- 로그인 POST /api/v2/ums/auth/login, 로그아웃 POST /api/v2/ums/auth/logout, 토큰검증 POST /api/v2/ums/auth/validate-token
- AuthState에 name 필드 추가, atomWithStorage에 getOnInit: true 적용
- 401 인터셉터: /ums/auth/ URL 제외, redirect 제거(authAtom 초기화만)
- AuthGate 컴포넌트로 앱 마운트 시 저장 토큰 검증 후 렌더링
- lib/error.ts(getErrorMessage)로 네트워크/서버/비즈니스 에러 분류
- 로그아웃: localStorage.removeItem + window.location.href 방식

### 회원가입 + 이메일 인증
- 회원가입 POST /api/v2/ums/user/join
- 단계적 폼: 이메일 입력 → 인증코드 발송 → 코드 입력/검증 → 인증 완료 시 이름·닉네임·비밀번호·회원가입 활성화
- apis/auth.ts: sendVerificationCode, verifyEmail (POST /api/v2/ums/auth/email-verification/send·verify)
- 뮤테이션: useSendVerificationCodeMutation, useVerifyEmailMutation, useJoinMutation
- 이메일 변경 시 인증 상태 초기화, 재발송 지원
- 회원가입 버튼은 인증 완료 전 비활성 (EMAIL_NOT_VERIFIED 사전 차단)

### 비밀번호 찾기
- 로그인 페이지에 "비밀번호를 잊으셨나요?" 링크 (/forgot-password)
- 단계적 폼: 이메일 → 코드 발송 → 코드 검증 → 새 비밀번호 설정
- apis/auth.ts: sendPasswordResetCode, verifyPasswordResetCode, confirmPasswordReset
- 뮤테이션: useSendPasswordResetCodeMutation, useVerifyPasswordResetCodeMutation, useConfirmPasswordResetMutation
- 폼 유효성 게이팅, 이메일 변경 시 단계 초기화, 성공 시 로그인 이동

### 설정 - 회원정보 조회
- GET /api/v2/ums/user/me 연동 (useUserQuery)
- 설정 페이지 아바타 하단에 닉네임 표시

### 설정 - 회원정보 수정
- 회원정보 수정 페이지 (이메일/이름 읽기전용, 닉네임 변경 폼)
- PATCH /api/v2/ums/user/me 연동
- 수정 성공 시 설정 페이지 이동 + 닉네임 갱신

### 설정 - 비밀번호 변경
- 비밀번호 변경 페이지 (현재/새/확인 입력)
- PATCH /api/v2/ums/user/me/password 연동 (useChangePasswordMutation)

### 설정 - 회원탈퇴
- 설정 페이지 회원탈퇴 버튼 → 확인 모달 → DELETE /api/v2/ums/user/me (useWithdrawMutation)
- 성공 시 localStorage('auth') 제거 + /login 이동
- 백엔드가 개발 단계 하드 삭제라 탈퇴 후 같은 이메일 재가입 가능

### 기술 커뮤니티 (피드/작성/상세)
- 기술 서브탭에 커뮤니티·채용 추가, 최상위 탭 순서 변경(기술 우선) + 기본 진입 /tech
- 피드 (/tech/community): PostCard 목록 + 무한 스크롤 + 하단 작성바 + 맨 위로 FAB
- 글 작성 (/tech/community/write): 제목(optional) + 내용 + 카테고리(최대3) + 풀스크린 오버레이
- 글 상세 (/tech/community/[postId]): 본문 + 액션행 + 댓글 목록/입력 + 풀스크린 오버레이
- 카테고리: 필터(전체+7) 단일선택 / 작성 다중 태깅(최대3) / 배지
- 기술 서브탭 헤더 스크롤 collapse (SubTabLayout collapseOnScroll)
- 피드 백엔드 연동: 목록 API(GET /api/v2/pms/{section}/posts) cursor 무한스크롤(useInfiniteQuery, nextCursor 기준), categoryId 서버 필터, PostCard를 PostSummary 기반·상대시간(lib/date.formatRelativeTime)으로 전환, 작성 후 invalidate, 피드 더미 atom 제거(내 글 더미만 유지) — web-v1과 동일
- 게시글/댓글 타입 + Jotai atom(더미 시드, 내 글 더미만 유지)
- web-v1과 동일 기능 (라우팅만 App Router 방식, 작성/상세는 fixed 오버레이로 전체화면)

### 공통 UI/UX
- 입력 폼 유효성 통과 시에만 제출 버튼 활성화 (로그인/회원가입/비밀번호 변경/회원정보 수정)
- antd App 래퍼 적용(모달/메시지 테마 일관)
- 링크/활성 칩 분홍 테마(#f2a0b5) 통일, 필터 칩 가로 스크롤

### 개발 환경 / 테스트
- 테스트 환경 구성: vitest + @testing-library/react + msw
- MSW 핸들러(login/validate-token/logout 등) + 인증/페이지 테스트(auth.test.ts, LoginPage.test.tsx, page.test.tsx, forgot-password/page.test.tsx)
- 피드/작성/상세 테스트

### Observability (server-side trace + metrics)
- Next.js instrumentation 엔트리 추가 (src/instrumentation.ts)
- Node runtime tracing 초기화 (src/instrumentation.node.ts) — Node runtime에서만 SDK 초기화
- Grafana Cloud OTLP trace export 의존성 추가, direct push
- 환경변수 체계 정리: APP_ENV는 런타임 주입, NEXT_PUBLIC_APP_ENV는 .env 유지로 역할 분리, OTEL_EXPORTER_OTLP_*
- Docker 빌드를 Yarn Berry non-zero-install 기준으로 수정, Next.js standalone 출력 제거

## 설계 근거

### 환경 분리
- .env.development(로컬), .env.staging(스테이징), .env.production(프로덕션)
- 환경 변수 prefix: NEXT_PUBLIC_ (레거시는 VITE_)
- 주요 변수: NEXT_PUBLIC_APP_ENV, API URL, AUTH TOKEN KEY
- 서버 런타임 env(APP_ENV)는 .env가 아니라 실행 주체(package script, ECS)가 주입
- 환경명은 APP_ENV 우선, 없으면 NEXT_PUBLIC_APP_ENV fallback

### 페이지 구성
- / : 메인 페이지
- /test : 테스트 페이지

### Dockerfile
- multi-stage 빌드 (의존성 설치 + 빌드 → 실행)
- Yarn Berry는 사용하되 zero-install은 사용하지 않음 (빌드 컨테이너 내부에서 yarn install --immutable)
- Next.js는 일반 next start 방식으로 실행 (standalone 미사용)
- 빌드 시 ENV arg로 환경 지정, 포트 3000

### 인증 설계 (web-v1 이슈 반영)
- atomWithStorage getOnInit: true → 새 탭/새로고침 시 null 방지
- AuthGate 패턴 → 앱 마운트 시 저장된 토큰 검증 후 렌더링
- 401 인터셉터에서 /ums/auth/ URL 제외 → 로그인 실패 시 auth 초기화 방지
- 401 인터셉터에서 redirect 제거 → authAtom 초기화만
- 로그아웃: localStorage.removeItem + window.location.href → RequireAuth 타이밍 이슈 회피
- Next.js 차이점
  - AuthGate를 providers.tsx에서 래핑 (web-v1은 BrowserRouter 밖)
  - RequireAuth는 children prop 기반 (web-v1은 Outlet 기반)
  - window 접근은 'use client' 컴포넌트에서만 가능

### Observability 설계
- 1차는 server-side trace만 적용 (확장으로 metrics 추가)
- src/instrumentation.ts에서 Next.js instrumentation register, Node runtime에서만 OpenTelemetry SDK 초기화
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS 기반 exporter 구성 (trace, metrics 공유). endpoint 없으면 setup 생략
- Trace: Next.js App Router 요청/렌더링/fetch span 수집 → Grafana Cloud OTLP direct push
- Metrics: NodeSDK 직접 구성으로 direct push
  - 프로세스 메트릭: @opentelemetry/host-metrics (process.cpu.utilization, process.memory.usage)
  - HTTP 메트릭: @opentelemetry/instrumentation-http 추가했으나 Next.js 환경에서 http.server.request.duration 미생성 확인(한계). 요청 수는 현재 trace 기반으로 확인
- Docker 빌드는 Yarn Berry non-zero-install 기준 유지, Next.js는 standalone 없이 일반 서버 실행
- 서버 런타임 env(APP_ENV)와 public env(NEXT_PUBLIC_APP_ENV) 분리 관리
- production 장기 권장안은 Alloy지만 현재는 direct push
