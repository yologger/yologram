## Observability

- [x] Next.js instrumentation 엔트리 추가 (src/instrumentation.ts)
- [x] Node runtime tracing 초기화 추가 (src/instrumentation.node.ts)
- [x] Grafana Cloud OTLP trace export 의존성 추가
- [x] trace용 환경변수 체계 정리 (APP_ENV, OTEL_EXPORTER_OTLP_*)
- [x] Docker 빌드를 Yarn Berry non-zero-install 기준으로 수정
- [x] Next.js standalone 출력 제거
- [x] APP_ENV는 런타임 주입, NEXT_PUBLIC_APP_ENV는 .env 유지로 역할 분리
- [ ] staging/prod 런타임에 OTEL secret 주입
- [ ] Grafana Tempo에서 trace 수신 확인
- [ ] GitHub Actions 빌드 캐시 적용

## 공통 기능

- [x] 입력 폼 유효성 통과 시 제출 버튼 활성화 (로그인/회원가입/비밀번호 변경/회원정보 수정)
- [ ] 코드 발송 버튼 재발송 쿨다운 (localStorage 기반, 새로고침 유지, forgot-password·join 공통)

## 인증

- [x] 회원가입 API 연동 (POST /api/v2/ums/user/join)
- [x] JoinPage, useJoinMutation, apis/auth.ts join 함수
- [x] lib/error.ts 생성 (getErrorMessage)
- [x] AuthState에 name 필드 추가, atomWithStorage에 getOnInit: true
- [x] login() 실제 API 호출로 교체
- [x] logout() 실제 API 호출로 교체
- [x] validateToken() 함수 추가
- [x] 401 인터셉터 수정 (/ums/auth/ 제외, redirect 제거)
- [x] AuthGate 컴포넌트 추가
- [x] useLoginMutation: getErrorMessage 적용
- [x] useLogoutMutation: localStorage.removeItem + window.location.href 방식
- [x] useJoinMutation: getErrorMessage 적용
- [x] LoginPage 테스트 힌트 제거
- [x] MSW 핸들러 추가 (login, validate-token, logout)
- [x] auth.test.ts 로그인/토큰검증/로그아웃 테스트
- [x] LoginPage.test.tsx 생성

## 설정 - 회원정보 조회

- [x] GET /api/v2/ums/user/me 연동 (useUserQuery)
- [x] 설정 페이지 아바타 하단에 닉네임 표시

## 설정 - 회원정보 수정

- [x] 회원정보 수정 페이지 (이메일/이름 읽기전용, 닉네임 변경 폼)
- [x] PATCH /api/v2/ums/user/me 연동
- [x] 수정 성공 시 설정 페이지 이동 + 닉네임 갱신

## 설정 - 비밀번호 변경

- [x] 비밀번호 변경 페이지 (현재/새/확인 입력)
- [x] PATCH /api/v2/ums/user/me/password 연동 (useChangePasswordMutation)

## 설정 - 회원탈퇴

- [x] 설정 페이지에 회원탈퇴 버튼 + 확인 모달
- [x] DELETE /api/v2/ums/user/me 연동 (useWithdrawMutation)
- [x] 탈퇴 성공 시 localStorage 정리 + 로그인 페이지 이동

## 이메일 인증

- [x] 회원가입 폼에 이메일 인증 단계 추가
- [x] 인증 코드 발송 API 연동 (POST /api/v2/ums/auth/email-verification/send)
- [x] 인증 코드 입력/검증 UI (POST /api/v2/ums/auth/email-verification/verify)
- [x] 인증 완료 후 가입 폼 활성화 + 회원가입 진행
- [x] 이메일 인증 테스트 (page.test.tsx, auth.test.ts)

## 비밀번호 찾기

- [x] 로그인 페이지에 비밀번호 찾기 링크 (/forgot-password)
- [x] forgot-password 단계적 폼 (이메일 → 코드 발송/검증 → 새 비밀번호)
- [x] apis/auth.ts + 뮤테이션 훅 3개 (send/verify/confirm)
- [x] MSW 핸들러 + 테스트 (forgot-password/page.test.tsx, auth.test.ts)

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
- [x] 회원가입 테스트 (auth.test.ts, page.test.tsx)

## 기타

- [ ] Next.js 서버 컴포넌트 / 클라이언트 컴포넌트 완벽히 이해하기
- [ ] 인증: cookie 기반 토큰 전환 시 middleware 방식으로 route 보호 검토
- [ ] route group 구조 검토
