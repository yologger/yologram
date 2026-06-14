# yologram-web-v1 구현 계획

## 1단계: 회원가입 (완료)

- JoinPage 폼 (이메일, 이름, 닉네임, 비밀번호)
- apis/auth.ts join 함수 → POST /api/v1/ums/user/join
- useJoinMutation: 성공 시 /login 이동, 실패 시 서버 에러 메시지 표시
- 프론트 validation은 서버와 동일 (이메일 형식, 이름/닉네임 2~20자, 비밀번호 8~20자)

## 2단계: 로그인/로그아웃/토큰검증

- auth.ts: login() 실제 API 호출로 교체, logout() 실제 API 호출, validateToken() 추가
- AuthState에 name 필드 추가 (API 응답에 포함)
- LoginPage 테스트 힌트 텍스트 제거
- 401 인터셉터에서 authAtom 초기화 (토큰 만료/무효 시 자동 로그아웃)
- MSW 핸들러 추가 (login, logout, validate-token)
- 테스트 작성 (API 함수, LoginPage)
- AuthGate로 앱 시작 시 저장 토큰 검증 후 라우터 렌더링
- authAtom 첫 렌더 localStorage rehydrate 보강

## 3단계: 회원정보 조회

- 설정 페이지 아바타 하단에 닉네임 표시 (GET /api/v1/ums/user/me 연동)
- useUserQuery 훅 생성

## 4단계: 회원정보 수정

- 설정 > 회원정보 수정 페이지
- 이름, 닉네임 변경 폼 → PATCH /api/v1/ums/user/me 연동
- 수정 성공 시 설정 페이지로 이동 + 닉네임 갱신

## 5단계: 비밀번호 변경

- 설정 > 비밀번호 변경 페이지
- 현재 비밀번호, 새 비밀번호, 비밀번호 확인 3개 입력
- PATCH /api/v1/ums/user/me/password 연동

## 6단계: 이메일 인증 (완료)

- JoinPage 단계적 폼: 이메일 입력 → 인증코드 발송 → 코드 입력/검증 → 인증 완료 시 이름·닉네임·비밀번호·회원가입 활성화
- apis/auth.ts: sendVerificationCode, verifyEmail (POST /api/v1/ums/auth/email-verification/send·verify)
- useSendVerificationCodeMutation, useVerifyEmailMutation
- 이메일 변경 시 인증 상태 초기화, 재발송 지원
- 회원가입 버튼은 인증 완료 전 비활성 (EMAIL_NOT_VERIFIED 사전 차단)

## 7단계: 비밀번호 찾기 (완료)

- 로그인 페이지에 "비밀번호를 잊으셨나요?" 링크 (/forgot-password)
- ForgotPasswordPage 단계적 폼: 이메일 → 코드 발송 → 코드 검증 → 새 비밀번호 설정
- apis/auth.ts: sendPasswordResetCode, verifyPasswordResetCode, confirmPasswordReset
- useSendPasswordResetCodeMutation, useVerifyPasswordResetCodeMutation, useConfirmPasswordResetMutation
- 폼 유효성 게이팅(코드 발송/인증/변경 버튼), 이메일 변경 시 단계 초기화, 성공 시 로그인 이동

## 8단계: Refresh Token

- login 응답에서 refresh token 저장
- 401 시 refresh token으로 access token 재발급 후 재요청

## 회원탈퇴 (완료)

- 설정 페이지 회원탈퇴 → 확인 모달 → DELETE /api/v1/ums/user/me (useWithdrawMutation)
- 성공 시 localStorage('auth') 제거 + /login 이동
- 백엔드가 개발 단계 하드 삭제라 탈퇴 후 같은 이메일 재가입 가능

## 제외 범위 (이후)

- OAuth (Gmail, Kakao)
- 프로필 이미지 업로드
