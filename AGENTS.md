# yologram-v1 프로젝트 지침 (Codex)

## 프로젝트 구조

- 모노레포 구성
- backend/: Spring Boot MVC
- frontend/: React
- infra/: 인프라 (Terraform)
- .github/workflows/: GitHub Actions

## 인프라

- IaC: Terraform (루트 레벨 관리)
- web: S3 + CloudFront 또는 Amplify (미정)
- api: App Runner (고려 중)

## CI/CD

- GitHub Actions 사용
- backend/ 변경 시 API 전용 workflow 트리거
- frontend/ 변경 시 Web 전용 workflow 트리거

## 작업 규칙

- 코드 변경 전 반드시 plan을 먼저 보여주고, 승인 후 적용
- 코드 변경 시 README.md, CLAUDE.md, AGENTS.md를 함께 업데이트
