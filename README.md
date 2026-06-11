# yologram-infra

yologram AWS 인프라 관리 (Terraform).

## 구조

```
aws/
  global/
    vpc/                    # VPC (vpc-prod, 10.0.0.0/16)
    ecs/                    # ECS 클러스터 (ecs-prod, Fargate SPOT) + Cloud Map + IAM
    api-gateway/            # HTTP API Gateway (yologram-gateway), VPC Link, Custom Domain
    database/               # RDS MySQL 8.0 (db.t4g.micro)
    elasticache/            # Valkey 8.0 (cache.t3.micro)
    opensearch/             # OpenSearch 2.19 (t3.small.search)
  services/
    n8n/                    # n8n 워크플로우 자동화 (Lightsail)
    yologram-api-v1/        # Spring Boot API (ECS Fargate SPOT)
    yologram-api-v2/        # FastAPI (ECS Fargate SPOT)
    yologram-web-v1/        # S3 + CloudFront (SPA)
    yologram-web-v2/        # Next.js (ECS Fargate SPOT)
```
