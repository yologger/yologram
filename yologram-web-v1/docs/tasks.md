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

## 설정 - 회원정보 조회

- [ ] GET /api/v1/ums/user/me 연동 (useUserQuery)
- [ ] 설정 페이지 아바타 하단에 닉네임 표시

## 설정 - 회원정보 수정

- [ ] 회원정보 수정 페이지 (이름, 닉네임 변경 폼)
- [ ] PUT /api/v1/ums/user 연동
- [ ] 수정 성공 시 설정 페이지 이동 + 닉네임 갱신

## 설정 - 비밀번호 변경

- [ ] 비밀번호 변경 페이지 (현재/새/확인 입력)
- [ ] PUT /api/v1/ums/user/password 연동

## 이메일 인증

- [ ] 회원가입 폼에 이메일 인증 단계 추가
- [ ] 인증 코드 발송 API 연동
- [ ] 인증 코드 입력/검증 UI

## 비밀번호 찾기

- [ ] 로그인 페이지에 비밀번호 찾기 링크
- [ ] 비밀번호 찾기 페이지 (이메일 입력 → 재설정 요청)

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
