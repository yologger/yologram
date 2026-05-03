# yologram 프로젝트 지침 (Codex)

## 프로젝트 구조

- 모노레포 구성
- yologram-api-v1/: Spring Boot MVC (Kotlin)
- yologram-web-v1/: React
- yologram-api-v2/: FastAPI
- yologram-web-v2/: Next.js
- .github/workflows/: GitHub Actions

## 인프라

- IaC: Terraform (yologger-infra 레포에서 관리)
- v1 API: ECS Fargate

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

- 코드 변경 전 반드시 plan을 먼저 보여주고, 승인 후 적용
- 코드 변경 시 README.md, CLAUDE.md, AGENTS.md를 함께 업데이트
- 작업 전 해당 프로젝트의 docs/ (plan.md, tasks.md, brainstorm.md)를 참고할 것
