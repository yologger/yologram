# yologram 프로젝트 지침

## 프로젝트 구조

- 모노레포 구성
- v1/yologram-api-v1/: Spring Boot MVC (Kotlin)
- v1/yologram-web-v1/: React
- v2/yologram-api-v2/: FastAPI
- v2/yologram-web-v2/: Next.js
- .github/workflows/: GitHub Actions

## 인프라

- IaC: Terraform (yologger-infra 레포에서 관리)
- v1 API: ECS Fargate

## CI/CD

- GitHub Actions 사용
- v1/ 변경 시 v1 전용 workflow 트리거
- v2/ 변경 시 v2 전용 workflow 트리거
- ECR push 시 이미지 태그: {branch}-{commit SHA 8자리}

## 작업 규칙

- 코드 변경 전 반드시 plan을 먼저 보여주고, 승인 후 적용
- 코드 변경 시 README.md, CLAUDE.md, AGENTS.md를 함께 업데이트
- 작업 전 해당 프로젝트의 docs/ (plan.md, tasks.md, brainstorm.md)를 참고할 것
