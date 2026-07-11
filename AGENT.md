# AGENT.md

This file provides guidance to OpenAI Codex when working with code in this repository.

## 프로젝트 개요

yologram AWS 인프라 관리. Terraform으로 환경별/서비스별 리소스를 분리 관리.

## 구조

- aws/global/ - 환경 공통 리소스 (VPC, ECS 클러스터, API Gateway, Database, ElastiCache, OpenSearch, IAM)
- aws/services/ - 개별 서비스 인프라 (yologram-api-v1, yologram-api-v2, yologram-web-v1, yologram-web-v2, yologram-worker, yologram-admin-web)
- aws/tools/ - 운영 보조 도구 (n8n)
- 각 디렉토리가 독립된 terraform state를 가짐

## 공통

- AWS profile: yologram
- 리전: ap-northeast-2 (서울)
- state: 로컬
- terraform apply는 사용자가 직접 실행. Codex는 코드 변경까지만 수행

## Global

### VPC (aws/global/vpc/)
- CIDR: 10.0.0.0/16, VPC 태그: vpc-prod
- 서브넷: pub-a (10.0.1.0/24, ap-northeast-2a), pub-b (10.0.2.0/24, ap-northeast-2b)

### ECS (aws/global/ecs/)
- ECS 클러스터: ecs-prod (FARGATE_SPOT)
- Cloud Map: ecs-prod.internal (새 VPC에 연결)
- IAM: ecs-task-execution-role

### API Gateway (aws/global/api-gateway/)
- HTTP API: yologram-gateway
- 커스텀 도메인: api.yologram.link
- VPC Link: prod-vpc-link (새 VPC 서브넷 연결)

### Database (aws/global/database/)
- RDS MySQL 8.0, db.t4g.micro (프리티어)
- 인스턴스: mysql-prod, username: master
- storage_encrypted: false (크로스 계정 스냅샷 이전 고려)
- apply 시 db_password 입력 필요

### ElastiCache (aws/global/elasticache/)
- Valkey 8.0, cache.t4g.micro (프리티어)

### OpenSearch (aws/global/opensearch/)
- OpenSearch 2.19, t3.small.search (프리티어)
- Public access (VPC 외부 접근 가능)
- fine-grained access control 활성화, master 유저
- apply 시 opensearch_master_password 입력 필요

## Tools

### n8n (aws/tools/n8n/)
- AWS Lightsail Instance (Amazon Linux 2023, micro_3_0, $5/mo)
- Docker Compose로 n8n + Caddy 구성 (user_data로 자동 프로비저닝)
- 도메인: n8n.yologram.link (Route 53 A 레코드)
- HTTPS: Caddy가 Let's Encrypt 인증서 자동 발급/갱신
- n8n DB: 내장 SQLite (/opt/n8n/data)
- 방화벽: 80(HTTP), 443(HTTPS)만 개방. SSH는 Lightsail 브라우저 접속
- Lightsail은 VPC 무관

## Services

### yologram-api-v1 (aws/services/yologram-api-v1/)
- ECS Fargate SPOT (0.25 vCPU, 512MB)
- Spring Boot, 컨테이너 포트 5000
- ECR: yologram-api-v1
- API Gateway 경로: api.yologram.link/api/v1/*
- Cloud Map 서비스 디스커버리로 API Gateway 연결
- SSM(prod): Grafana OTLP (metrics/traces/logs) + DB writer/reader 접속정보 + JWT secret
- 컨테이너 환경변수는 SPRING_PROFILES_ACTIVE만 주입, 나머지는 앱이 SSM에서 직접 read
- vpc_link_id 변수 필요

### yologram-api-v2 (aws/services/yologram-api-v2/)
- ECS Fargate SPOT (0.25 vCPU, 512MB)
- FastAPI (Python), 컨테이너 포트 5000
- ECR: yologram-api-v2
- API Gateway 경로: api.yologram.link/api/v2/*
- Cloud Map 서비스 디스커버리로 API Gateway 연결
- SSM: OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS, DB_URL/DB_USERNAME/DB_PASSWORD, JWT_SECRET
- 환경변수: APP_PROFILE=prod (DB/JWT/OTEL은 SSM SecureString으로 컨테이너 주입)
- vpc_link_id 변수 필요

### yologram-web-v1 (aws/services/yologram-web-v1/)
- S3 + CloudFront (SPA)
- 도메인: web.v1.yologram.link
- ACM 인증서: us-east-1 (CloudFront 요구사항)
- VPC 무관

### yologram-web-v2 (aws/services/yologram-web-v2/)
- ECS Fargate SPOT (0.25 vCPU, 512MB)
- Next.js, 컨테이너 포트 3000
- ECR: yologram-web-v2
- 커스텀 도메인: web.v2.yologram.link (API Gateway)
- Cloud Map 서비스 디스커버리로 API Gateway 연결
- SSM: OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_EXPORTER_OTLP_HEADERS
- 환경변수: APP_ENV=production
- vpc_link_id 변수 필요

### yologram-admin-web (aws/services/yologram-admin-web/)
- S3 + CloudFront (SPA)
- 도메인: admin.yologram.link
- ACM 인증서: us-east-1 (CloudFront 요구사항)
- VPC 무관

### yologram-worker (aws/services/yologram-worker/)
- ECS Fargate SPOT (0.25 vCPU, 512MB)
- Spring Boot 비동기 워커, 인바운드 트래픽 없음 (API Gateway·Cloud Map·portMappings 미사용, SG는 egress만)
- ECR: yologram-worker
- SSM(prod): Grafana OTLP (metrics/traces/logs) — DB·JWT는 필요 시(News) 추가
- 컨테이너 환경변수는 SPRING_PROFILES_ACTIVE만 주입, 나머지는 앱이 SSM에서 직접 read
- actuator(5000)는 ECS exec로 localhost 접근

## Terraform 명령어

```
cd aws/services/yologram-api-v1    # 각 디렉토리에서 개별 실행
terraform init
terraform plan
terraform apply
terraform destroy
```

## Data Source 패턴

VPC 및 서브넷 참조 시 data source 사용:
- VPC: tag Name=vpc-prod로 필터
- Subnet: vpc_id 필터 + tag Name으로 필터 (pub-a, pub-b)
- 서브넷 필터에 반드시 vpc_id를 포함할 것 (동일 이름 서브넷 충돌 방지)
- 계정 ID는 하드코딩하지 말고 data.aws_caller_identity.current.account_id 사용
