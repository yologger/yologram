# yologram-v1

모노레포 구성의 풀스택 프로젝트.

## 구조

- backend/: Spring Boot MVC
- frontend/: React
- infra/: 인프라 (Terraform)
- .github/workflows/: GitHub Actions

## 인프라

- IaC: Terraform (루트 레벨 관리)
- web: S3 + CloudFront 또는 Amplify (미정)
- api: App Runner (고려 중)

## CI/CD

- GitHub Actions (디렉토리별 독립 트리거)