## 레이아웃

- [x] 반응형 레이아웃 (모바일 탭바 + 데스크탑 사이드바)
- [x] 기본 페이지 구성 (투자, 정치, 알림, 설정)

## 인증

- [x] 회원가입 API 연동 (POST /api/v1/ums/user/join)
- [x] JoinPage, useJoinMutation, apis/auth.ts join 함수
- [ ] 로그인/로그아웃 API 연동 (api-v1 2단계 완료 후)

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
