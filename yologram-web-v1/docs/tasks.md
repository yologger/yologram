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

## 투자

## 정치

## 알림

## 설정

## 개발 환경

- [x] 테스트 환경 구성 (vitest + @testing-library/react + msw)
- [x] 테스트 유틸리티 (setup.ts, handlers.ts, server.ts, utils.tsx)
- [x] 회원가입 테스트 (JoinPage.test.tsx, auth.test.ts)
