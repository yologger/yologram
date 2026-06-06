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

## 3단계: 유저 기능

- 설정 페이지에서 프로필 조회
- 회원 탈퇴

## 제외 범위 (이후)

- OAuth (Gmail, Kakao)
- 프로필 이미지 업로드
