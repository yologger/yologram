# yologram-web-v2 할 일

앞으로 할 작업 체크리스트. 구현 완료된 기능·설계 근거는 features.md 참조.

## 공통 기능
- [ ] 코드 발송 버튼 재발송 쿨다운 (localStorage 기반, 새로고침 유지, forgot-password·join 공통)

## 기술 커뮤니티 (추후 확장)
- [ ] invest/politics 피드 연동
- [ ] 내 글 목록 API 연동
- [ ] 팔로우/리포스트/공유/이모지/정렬/작성 툴바 동작

## Refresh Token
- [ ] login 응답에서 refresh token 저장
- [ ] 401 시 refresh token으로 access token 재발급 후 재요청

## 설정 - 환경 설정
- [ ] 알림 설정 페이지
- [ ] 다크 모드 설정 페이지

## 설정 - 활동
- [ ] 내가 쓴 글 페이지
- [ ] 저장한 글 페이지

## Observability 운영
- [ ] staging/prod 런타임에 OTEL secret 주입
- [ ] Grafana Tempo에서 trace 수신 확인
- [ ] GitHub Actions 빌드 캐시 적용

## Next.js / 인증 구조 학습·검토
- [ ] Next.js 서버 컴포넌트 / 클라이언트 컴포넌트 완벽히 이해하기
- [ ] 인증: cookie 기반 토큰 전환 시 middleware 방식으로 route 보호 검토
- [ ] route group 구조 검토

## 보류/제외 (현재 범위 밖)

### Observability
- [ ] browser RUM
- [ ] client-side tracing
- [ ] logs
- [ ] custom metrics (이번 단계에서 추가하지 않음)
- 향후 개선 선택지: Grafana Alloy + spanmetrics로 trace에서 request 메트릭 추출, access log 보강, 최소 custom metric 추가
