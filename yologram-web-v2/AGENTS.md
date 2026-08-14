# yologram-web-v2 프로젝트 지침

## 프로젝트 개요

Next.js 16 기반 웹 프론트엔드. ECS Fargate에서 운영.

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- src/instrumentation.ts: Next.js instrumentation 진입점
- src/instrumentation.node.ts: OpenTelemetry NodeSDK 초기화 (traces, metrics, logs)
- src/lib/logger.ts: 서버사이드 로그 유틸 (logInfo, logError)
- src/app/api/health/route.ts: 헬스체크 API Route
- src/hooks/useRequireAuth.ts: 미인증 로그인 유도 공용 훅 — 모달("로그인이 필요해요") → /login?returnTo= 이동(내부 경로만 허용) → 로그인 후 원위치 복귀. 하트·댓글(포커스 시점)·글쓰기 진입·RequireAuth 가드에서 사용, 미인증 진입점은 disabled 대신 이 훅 사용이 규칙
- src/components/common/SearchBar.tsx·SectionKeywordPage.tsx: 섹션 검색바(데스크탑 인라인/모바일 오버레이, Enter 시 /{section}/keywords/{키워드}) + 결과 페이지. Next params는 인코딩 상태 — decodeURIComponent 필요
- src/components/search/: 검색 결과 — 대상별 탭(커뮤니티·뉴스)으로 나누고 각 탭이 자기 페이징·정렬을 갖는다. 무한 스크롤이 아니라 페이지 네비게이션(antd Pagination) — 검색은 총 건수가 정보이고 상세에서 돌아올 때 위치가 유지돼야 한다(피드와 의도적으로 다름). 정렬은 연관도순/최신순(백엔드 sort 파라미터와 같은 값). searchResultMock.ts는 UI 확인용이라 엔드포인트 연동 시 삭제
- src/app/(main)/{tech,invest,politics}/(tabs)/: 섹션 탭 라우트는 (tabs) route group — 탭 레이아웃(SubTabLayout·ComingSoon)이 keywords/ 등 비탭 라우트에 물리지 않게 격리 (URL 불변)

## 코드 컨벤션

- 입력 폼 제출 버튼은 클라이언트 유효성 검증 통과 시에만 활성화 (Ant Form은 useFormSubmittable 훅, 수동 상태 폼은 파생 isValid)

## 환경변수

- NEXT_PUBLIC_*: 빌드 시 인라인, 클라이언트용
- APP_ENV: 서버 런타임 전용, ECS에서 주입
- OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS: OpenTelemetry SDK가 자동으로 읽음

## Observability

- OpenTelemetry NodeSDK로 Grafana Cloud OTLP direct push
- 설정: src/instrumentation.ts → src/instrumentation.node.ts
- (exporter·메트릭 라이브러리·한계 상세는 README.md 참조)

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- vitest + jsdom + @testing-library/react + msw
- 테스트 유틸리티: src/test/ (setup.ts, handlers.ts, server.ts, utils.tsx)
- 테스트 파일은 소스 파일 옆에 배치 (colocation)
- yarn test (단일 실행), yarn test:watch (감시 모드)

## 배포

- Docker (Yarn Berry non-zero-install, next start)
- ECS Fargate
- GitHub Actions: Docker build → ECR push → ECS 재배포
