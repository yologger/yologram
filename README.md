# yologger-infra

yologger AWS 인프라 관리.

## 구조

```
prod/
  common/
    ecs/                    # ECS 클러스터 (Fargate SPOT) + Cloud Map + Task Execution Role
    api-gateway/            # HTTP API Gateway (yologram-gateway), VPC Link, Custom Domain
    database/
      service-prod/         # RDS MySQL 8.0 (db.t4g.micro)
  service/
    n8n/                    # n8n 워크플로우 자동화 (Lightsail)
    yologram-api-v1/        # Spring Boot API (ECS Fargate SPOT)
    yologram-api-v2/
    yologram-web-v1/
    yologram-web-v2/
global/
  iam/                      # IAM (github-actions-deployer)
```

## AWS

- AWS profile: yologram
- 리전: ap-northeast-2 (서울)
- state: 로컬

## Terraform

```bash
cd prod/service/n8n               # 각 디렉토리에서 개별 실행
terraform init
terraform plan
terraform apply
terraform destroy
```

## Service

- n8n - 워크플로우 자동화 (Lightsail, Docker Compose, Caddy + HTTPS, $5/mo)
- yologram-api-v1 - Spring Boot API (ECS Fargate SPOT, api.yologram.link/api/v1/*)

## Common

- ECS 클러스터: ecs-prod (Fargate SPOT)
- API Gateway: api.yologram.link (HTTP API, 스로틀링 3 req/sec)
- Database: RDS MySQL 8.0 (db.t4g.micro)

## Global

- IAM: github-actions-deployer (ECR push + ECS 배포)
