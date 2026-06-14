## 레이아웃

- [x] 반응형 레이아웃 (모바일 탭바 + 데스크탑 사이드바)
- [x] 기본 페이지 구성 (투자, 정치, 알림, 설정)

## 인증

- [x] 회원가입 API 연동 (POST /api/v1/ums/user/join)
- [x] JoinPage, useJoinMutation, apis/auth.ts join 함수
- [x] login() 실제 API 호출로 교체
- [x] logout() 실제 API 호출
- [x] validateToken() 추가
- [x] AuthState에 name 필드 추가
- [x] LoginPage 테스트 힌트 제거
- [x] MSW 핸들러 추가 (login, logout, validate-token)
- [x] 로그인/로그아웃 테스트 작성
- [x] AuthGate 추가 (저장 토큰 검증 후 라우터 렌더링)
- [x] 새 탭/새로고침 authAtom rehydrate 보강

## 공통 기능

- [ ] 다크모드 지원 (Ant Design theme + 사용자 설정 저장)
- [x] 전역 상태 관리 도입 (Jotai - authAtom)
- [x] 입력 폼 유효성 통과 시 제출 버튼 활성화 (로그인/회원가입/비밀번호 변경/회원정보 수정)
- [ ] 코드 발송 버튼 재발송 쿨다운 (localStorage 기반, 새로고침 유지, forgot-password·join 공통)

## 설정 - 회원정보 조회

- [x] GET /api/v1/ums/user/me 연동 (useUserQuery)
- [x] 설정 페이지 아바타 하단에 닉네임 표시

## 설정 - 회원정보 수정

- [ ] 회원정보 수정 페이지 (이름, 닉네임 변경 폼)
- [ ] PATCH /api/v1/ums/user/me 연동
- [ ] 수정 성공 시 설정 페이지 이동 + 닉네임 갱신

## 설정 - 비밀번호 변경

- [x] 비밀번호 변경 페이지 (현재/새/확인 입력)
- [x] PATCH /api/v1/ums/user/me/password 연동 (useChangePasswordMutation)

## 설정 - 회원탈퇴

- [x] 설정 페이지에 회원탈퇴 버튼 + 확인 모달
- [x] DELETE /api/v1/ums/user/me 연동 (useWithdrawMutation)
- [x] 탈퇴 성공 시 localStorage 정리 + 로그인 페이지 이동

## 이메일 인증

- [x] 회원가입 폼에 이메일 인증 단계 추가
- [x] 인증 코드 발송 API 연동 (POST /api/v1/ums/auth/email-verification/send)
- [x] 인증 코드 입력/검증 UI (POST /api/v1/ums/auth/email-verification/verify)
- [x] 인증 완료 후 가입 폼 활성화 + 회원가입 진행
- [x] 이메일 인증 테스트 (JoinPage.test.tsx, auth.test.ts)

## 비밀번호 찾기

- [x] 로그인 페이지에 비밀번호 찾기 링크 (/forgot-password)
- [x] ForgotPasswordPage 단계적 폼 (이메일 → 코드 발송/검증 → 새 비밀번호)
- [x] apis/auth.ts + 뮤테이션 훅 3개 (send/verify/confirm)
- [x] MSW 핸들러 + 테스트 (ForgotPasswordPage.test.tsx, auth.test.ts)

## Refresh Token

- [ ] login 응답에서 refresh token 저장
- [ ] 401 시 refresh token으로 access token 재발급 후 재요청

## 설정 - 환경 설정

- [ ] 알림 설정 페이지
- [ ] 다크 모드 설정 페이지

## 설정 - 활동

- [ ] 내가 쓴 글 페이지
- [ ] 저장한 글 페이지

## 개발 환경

- [x] 테스트 환경 구성 (vitest + @testing-library/react + msw)
- [x] 테스트 유틸리티 (setup.ts, handlers.ts, server.ts, utils.tsx)
- [x] 회원가입 테스트 (JoinPage.test.tsx, auth.test.ts)
