# yologram-web-v1 구현 계획

## 1단계: 회원가입 (완료)

- JoinPage 폼 (이메일, 이름, 닉네임, 비밀번호)
- apis/auth.ts join 함수 → POST /api/v1/ums/user/join
- useJoinMutation: 성공 시 /login 이동, 실패 시 서버 에러 메시지 표시
- 프론트 validation은 서버와 동일 (이메일 형식, 이름/닉네임 2~20자, 비밀번호 8~20자)

## 2단계: 로그인/로그아웃 (api-v1 2단계 완료 후)

- LoginPage에서 실제 API 호출로 전환 (현재 더미)
- 로그인 성공 시 JWT 토큰 저장 (Jotai atomWithStorage)
- 로그아웃 API 연동

## 3단계: 유저 기능

- 설정 페이지에서 프로필 조회
- 회원 탈퇴

## 제외 범위 (이후)

- OAuth (Gmail, Kakao)
- 프로필 이미지 업로드
