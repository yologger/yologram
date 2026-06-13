# yologram-web-v2 Plan

## 인증 - 로그인/로그아웃/토큰검증 API 연동

### 1단계: 공통 유틸리티
- lib/error.ts 생성 (getErrorMessage - 네트워크/서버/비즈니스 에러 분류)

### 2단계: 인증 상태 보강
- AuthState에 name 필드 추가
- atomWithStorage에 getOnInit: true 추가

### 3단계: API 함수 교체
- login() → POST /api/v2/ums/auth/login
- logout() → POST /api/v2/ums/auth/logout
- validateToken() 추가 → POST /api/v2/ums/auth/validate-token

### 4단계: 401 인터셉터 수정
- /ums/auth/ URL 제외
- window.location.href = '/login' 제거 (authAtom 초기화만)

### 5단계: AuthGate 추가
- providers.tsx에서 AuthGate로 감싸기
- 앱 마운트 시 저장된 토큰 검증 후 라우터 렌더링

### 6단계: Mutation 훅 수정
- useLoginMutation: getErrorMessage 적용
- useLogoutMutation: localStorage.removeItem + window.location.href 방식
- useJoinMutation: getErrorMessage 적용

### 7단계: UI 수정
- LoginPage 테스트 힌트 제거

### 8단계: 테스트
- MSW 핸들러 추가 (login, validate-token, logout)
- auth.test.ts 로그인/토큰검증/로그아웃 테스트
- LoginPage.test.tsx 생성

## 회원정보 조회

- 설정 페이지 아바타 하단에 닉네임 표시 (GET /api/v2/ums/user/me 연동)
- useUserQuery 훅 생성

## 회원정보 수정

- 설정 > 회원정보 수정 페이지
- 이름, 닉네임 변경 폼 → PATCH /api/v2/ums/user/me 연동
- 수정 성공 시 설정 페이지로 이동 + 닉네임 갱신

## 비밀번호 변경

- 설정 > 비밀번호 변경 페이지
- 현재 비밀번호, 새 비밀번호, 비밀번호 확인 3개 입력
- PATCH /api/v2/ums/user/me/password 연동

## 이메일 인증 (완료)

- join 단계적 폼: 이메일 입력 → 인증코드 발송 → 코드 입력/검증 → 인증 완료 시 이름·닉네임·비밀번호·회원가입 활성화
- apis/auth.ts: sendVerificationCode, verifyEmail (POST /api/v2/ums/auth/email-verification/send·verify)
- useSendVerificationCodeMutation, useVerifyEmailMutation
- 이메일 변경 시 인증 상태 초기화, 재발송 지원
- 회원가입 버튼은 인증 완료 전 비활성 (EMAIL_NOT_VERIFIED 사전 차단)

## 비밀번호 찾기 (완료)

- 로그인 페이지에 "비밀번호를 잊으셨나요?" 링크 (/forgot-password)
- forgot-password 단계적 폼: 이메일 → 코드 발송 → 코드 검증 → 새 비밀번호 설정
- apis/auth.ts: sendPasswordResetCode, verifyPasswordResetCode, confirmPasswordReset
- useSendPasswordResetCodeMutation, useVerifyPasswordResetCodeMutation, useConfirmPasswordResetMutation
- 폼 유효성 게이팅, 이메일 변경 시 단계 초기화, 성공 시 로그인 이동

## Refresh Token

- login 응답에서 refresh token 저장
- 401 시 refresh token으로 access token 재발급 후 재요청

---

# yologram-web-v2 Observability Plan

## Trace

- server-side trace 적용
- Next.js App Router 요청/렌더링/fetch span 수집
- Grafana Cloud OTLP endpoint로 direct push

## Metrics

- NodeSDK 직접 구성으로 Grafana Cloud OTLP endpoint에 direct push
- 프로세스 메트릭: @opentelemetry/host-metrics (process.cpu.utilization, process.memory.usage)
- HTTP 메트릭: @opentelemetry/instrumentation-http 추가했으나 Next.js 환경에서 http.server.request.duration 미생성 확인 (한계)
- 요청 수는 http.server.request.duration count 기반이 목표였으나, 현재는 trace 기반으로 확인

## 구현 방향

- `src/instrumentation.ts`에서 Next.js instrumentation register
- Node runtime에서만 OpenTelemetry SDK 초기화
- `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_EXPORTER_OTLP_HEADERS` 기반 exporter 구성 (trace, metrics 공유)
- endpoint가 없으면 trace/metrics setup 생략
- Docker 빌드는 Yarn Berry non-zero-install 기준으로 유지
- Next.js 배포는 `standalone` 없이 일반 서버 실행으로 유지
- 서버 런타임 env(`APP_ENV`)와 public env(`NEXT_PUBLIC_APP_ENV`)를 분리 관리
- production 장기 권장안은 Alloy지만 현재는 direct push

## 제외 범위

- browser RUM
- client-side tracing
- logs
- custom metrics (이번 단계에서 추가하지 않음)

## 향후 개선 선택지

- Grafana Alloy + spanmetrics로 trace에서 request 메트릭 추출
- access log 보강
- 최소 custom metric 추가
