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

- 코드 개발/수정 시 관련된 루트 프로젝트와 하위 프로젝트의 문서(README.md, CLAUDE.md, AGENTS.md, docs/ 하위 파일)를 함께 최신화할 것
- 각 프로젝트 docs/는 tasks.md(앞으로 할 일 체크리스트)와 features.md(구현된 기능 + 설계 근거) 2개로 관리
- 브레인스토밍·플랜은 세션 대화로 진행하고, 결정된 "실제 할 일"만 docs/tasks.md에 추가. 구현 완료 후 그 기능·설계 근거를 features.md에 기록
- 코드 변경 전 반드시 plan을 먼저 보여주고, 승인 후 적용
- commit은 사용자가 요청하거나 승인한 경우에만 수행
- 코드 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- 신규 API 추가 시 Swagger 문서화 필수
- 작업 전 해당 프로젝트의 docs/ (tasks.md=할 일, features.md=구현 기능·설계 근거)를 참고할 것
- observability는 OpenTelemetry 기준으로 구성하고, 가능하면 기존 서비스와 동일하게 Grafana Cloud OTLP direct push 패턴을 우선 검토할 것
