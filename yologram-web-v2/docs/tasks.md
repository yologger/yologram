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

## 인증

- [x] 회원가입 API 연동 (POST /api/v2/ums/user/join)
- [x] JoinPage, useJoinMutation, apis/auth.ts join 함수
- [ ] 로그인/로그아웃 API 연동 (api-v2 2단계 완료 후)

## 개발 환경

- [x] 테스트 환경 구성 (vitest + @testing-library/react + msw)
- [x] 회원가입 테스트 (auth.test.ts, page.test.tsx)

## 기타

- [ ] Next.js 서버 컴포넌트 / 클라이언트 컴포넌트 완벽히 이해하기
- [ ] 인증: cookie 기반 토큰 전환 시 middleware 방식으로 route 보호 검토
- [ ] route group 구조 검토
