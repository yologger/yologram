# yologram

모노레포 구성의 풀스택 프로젝트. 동일 로직을 다른 기술 스택으로 구현.

## 구조

- v1/yologram-api-v1/: Spring Boot MVC (Kotlin)
- v1/yologram-web-v1/: React
- v2/yologram-api-v2/: FastAPI
- v2/yologram-web-v2/: Next.js
- .github/workflows/: GitHub Actions

## 인프라

- IaC: Terraform (yologger-infra 레포에서 관리)
- v1 API: ECS Fargate
- v2: 미정

## CI/CD

- GitHub Actions (디렉토리별 독립 트리거)
- ECR push 시 이미지 태그: {branch}-{commit SHA 8자리}

## ECS 컨테이너 접속

```bash
aws ecs execute-command \
  --cluster prod \
  --task <task-id> \
  --container yologram-api-v1 \
  --interactive \
  --command "/bin/sh" \
  --profile yologram \
  --region ap-northeast-2
```
