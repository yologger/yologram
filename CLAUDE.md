# yologram 프로젝트 지침

## 프로젝트 구조

- 모노레포 구성
- yologram-api-v1/: Spring Boot MVC (Kotlin)
- yologram-web-v1/: React
- yologram-api-v2/: FastAPI
- yologram-web-v2/: Next.js
- yologram-admin-web/: 어드민 웹 (예정)
- .github/workflows/: GitHub Actions

## 인프라

- IaC: Terraform (~/Workspace/yologger/yologger-infra/ 에서 관리)
- ECS Fargate: api-v1(5000), api-v2(5000), web-v2(3000)
- API Gateway: api.yologram.link → /api/v1/{proxy+}는 api-v1, /api/v2/{proxy+}는 api-v2, /{proxy+}는 web-v2
- web-v1: S3 + CloudFront
- 검색: OpenSearch (추후 도입 예정)

## CI/CD

- GitHub Actions 사용
- yologram-api-v1/ 변경 시 yologram-api-v1 workflow 트리거
- yologram-web-v1/ 변경 시 yologram-web-v1 workflow 트리거
- yologram-api-v2/ 변경 시 yologram-api-v2 workflow 트리거
- yologram-web-v2/ 변경 시 yologram-web-v2 workflow 트리거
- ECR push 시 이미지 태그: {branch}-{commit SHA 8자리}

## 커밋 메시지 컨벤션

- 형식: [프로젝트명] 타입: 설명
- 예시: [yologram-api-v1] feat: actuator 의존성 추가
- 여러 프로젝트에 걸친 변경: [all] chore: 프로젝트 구조 변경

## 작업 규칙

- CLAUDE.md는 Claude Code용, AGENTS.md는 Codex용으로 둘 다 둔다(번갈아 사용). 두 파일은 항상 동일 내용을 유지하며, 한쪽을 수정하면 다른 쪽도 즉시 동일하게 반영한다(루트·각 프로젝트 모두)
- 코드 개발/수정 시 관련 문서(README.md, CLAUDE.md, AGENTS.md, 루트 docs/)를 함께 최신화할 것
- 문서는 루트 docs/에 통합 관리: docs/todos.md(구현해야 할 기능) + docs/done.md(구현 완료된 기능 + 설계 근거) + docs/rules.md(구현 시 따라야 할 제약·참고사항). 모두 프로젝트 구분 없는 평면 구조 — todos.md는 우선순위 순 기능 목록(미러링 기능은 프로젝트별 하위 체크, 도메인 태그는 선택), done.md는 구현된 기능(대략 구현 순서) + 설계 근거(주제별), rules.md는 경로 규칙·호출 기준 등 구현 시 지켜야 할 제약·참고사항
- 브레인스토밍·플랜은 세션 대화로 진행하고, 결정된 "실제 할 일"만 docs/todos.md에 추가. 구현 완료 후 그 기능·설계 근거를 docs/done.md에 기록
- docs는 메인(루트) 에이전트만 갱신한다. 멀티에이전트 병렬 작업 시 서브에이전트는 docs read-only(참고만), 코드 변경 결과만 보고 — 단일 writer 유지로 충돌 방지
- 코드 변경 전 반드시 plan을 먼저 보여주고, 승인 후 적용
- commit은 사용자가 요청하거나 승인한 경우에만 수행
- 코드 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- 신규 API 추가 시 Swagger 문서화 필수
- 작업 전 루트 docs/ (todos.md=할 일, done.md=구현 기능·설계 근거, rules.md=제약·참고사항)를 참고할 것
- observability는 OpenTelemetry 기준으로 구성하고, 가능하면 기존 서비스와 동일하게 Grafana Cloud OTLP direct push 패턴을 우선 검토할 것
- 기능 구현·수정 후 curl로 API를 직접 호출해 검증한다. 이때 상태코드만 축약하지 말고 실제 응답 본문(JSON, 204는 상태 라인)을 항상 함께 보여준다. 데이터 변경 검증은 삭제/수정 전후 조회 결과까지 보여 실제 반영을 확인한다. 로컬 서버(api-v1:5001 / api-v2:5002) 실행·재기동은 사용자가 수행하므로, curl 전 서버가 떠 있는지 확인하고 안 떠 있으면 사용자에게 재기동을 요청한다. 조회(GET)는 자유 호출하되 데이터를 바꾸는 요청(작성/수정/삭제 POST·PATCH·DELETE)은 공유 RDS 변경이므로 사용자 승인 후 실행
