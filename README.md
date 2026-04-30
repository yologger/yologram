# yologger-infra

yologger AWS 인프라 관리.

## 구조

```
service/
  n8n/              # n8n 워크플로우 자동화 (Lightsail)
  openclaw/
global/
  iam/              # IAM 관리
  route53/          # 도메인, DNS
  database/
    service-prod/   # RDS MySQL (db.t4g.micro)
```

## AWS

- AWS profile: yologram
- 리전: ap-northeast-2 (서울)
- state: 로컬

## Terraform

```bash
cd service/n8n                    # 또는 global/database/service-prod 등
terraform init                    # 초기화
terraform plan                    # 변경사항 확인
terraform apply                   # 배포
terraform destroy                 # 전체 삭제
```

## Service

- n8n - 워크플로우 자동화 (Lightsail Instance, Docker Compose, Caddy + HTTPS, $5/mo)
- openclaw

## Global

- database/service-prod - RDS MySQL 8.0 (db.t4g.micro)
