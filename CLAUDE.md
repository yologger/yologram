# yologram 프로젝트 지침

## 프로젝트 구조

- 모노레포 구성
- yologram-api-v1/: Spring Boot MVC (Kotlin)
- yologram-web-v1/: React
- yologram-api-v2/: FastAPI
- yologram-web-v2/: Next.js
- .github/workflows/: GitHub Actions

## 인프라

- IaC: Terraform (~/Workspace/yologger/yologger-infra/ 에서 관리)
- ECS Fargate: api-v1(5000), api-v2(5000), web-v2(3000)
- API Gateway: api.yologram.link → /api/v1/{proxy+}는 api-v1, /api/v2/{proxy+}는 api-v2, /{proxy+}는 web-v2
- web-v1: S3 + CloudFront

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

- 기능 구현 시 docs/ (brainstorm.md, plan.md, tasks.md)와 README.md, CLAUDE.md, AGENTS.md를 먼저 업데이트한 후 코드 작성
- 코드 변경 전 반드시 plan을 먼저 보여주고, 승인 후 적용
- 코드 구현 시 항상 테스트코드도 함께 작성
- 작업 전 해당 프로젝트의 docs/ (plan.md, tasks.md, brainstorm.md)를 참고할 것
- observability는 OpenTelemetry 기준으로 구성하고, 가능하면 기존 서비스와 동일하게 Grafana Cloud OTLP direct push 패턴을 우선 검토할 것
- Next.js 계열 프로젝트에서는 서버 런타임 env와 `NEXT_PUBLIC_*` env를 분리해서 관리할 것
