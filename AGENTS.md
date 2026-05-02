# yologram 프로젝트 지침 (Codex)

## 프로젝트 구조

- 모노레포 구성
- v1/frontend/: React (S3 + CloudFront 배포)
- v1/backend/: Spring Boot MVC
- v1/infra/: Terraform
- v2/frontend/: Next.js (Lightsail 배포)
- v2/backend/: FastAPI (Lightsail 배포)
- v2/infra/: Terraform
- .github/workflows/: GitHub Actions

## 인프라

- IaC: Terraform
- v1: S3 + CloudFront (web), App Runner (api) — 고려 중
- v2: Lightsail (Docker Compose + Nginx)

## CI/CD

- GitHub Actions 사용
- v1/ 변경 시 v1 전용 workflow 트리거
- v2/ 변경 시 v2 전용 workflow 트리거

## 작업 규칙

- 코드 변경 전 반드시 plan을 먼저 보여주고, 승인 후 적용
- 코드 변경 시 README.md, CLAUDE.md, AGENTS.md를 함께 업데이트
