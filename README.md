# yologram

모노레포 구성의 풀스택 프로젝트. 동일 로직을 다른 기술 스택으로 구현.

## 구조

- v1/: React + Spring Boot MVC
  - frontend/: React (S3 + CloudFront 배포)
  - backend/: Spring Boot MVC
  - infra/: Terraform
- v2/: Next.js + FastAPI
  - frontend/: Next.js (Lightsail 배포)
  - backend/: FastAPI (Lightsail 배포)
  - infra/: Terraform

## 인프라

- IaC: Terraform
- v1: S3 + CloudFront (web), App Runner (api) — 고려 중
- v2: Lightsail (Docker Compose + Nginx)

## CI/CD

- GitHub Actions (디렉토리별 독립 트리거)
