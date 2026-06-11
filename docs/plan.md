# yologram-infra 구현 계획

## 현재 구축 완료

### Global (aws/global/)
- VPC: 10.0.0.0/16, pub-a/pub-b 서브넷
- ECS: 클러스터 ecs-prod (FARGATE_SPOT), Cloud Map ecs-prod.internal, ecs-task-execution-role
- API Gateway: HTTP API yologram-gateway, 커스텀 도메인 api.yologram.link, prod-vpc-link
- Database: RDS MySQL 8.0 (db.t4g.micro)
- ElastiCache: Valkey 8.0 (cache.t3.micro)
- OpenSearch: 2.19 (t3.small.search)

### Services (aws/services/)
- n8n: Lightsail + Docker Compose (n8n + Caddy)
- yologram-api-v1: ECS Fargate, Spring Boot, 포트 5000
- yologram-api-v2: ECS Fargate, FastAPI, 포트 5000
- yologram-web-v1: S3 + CloudFront
- yologram-web-v2: ECS Fargate, Next.js, 포트 3000

## 1단계: 보안/정리 (진행 중)

- 계정 ID 하드코딩 제거 (data source 치환) — 완료
- git 히스토리 계정 ID 제거 — 완료
- *.tfvars gitignore 처리 — 완료
- IAM github-actions-deployer 최소권한화
- GitHub Actions IAM User → OIDC 전환 검토

## 2단계: 외부 서비스 IaC 편입 검토

### GitHub (integrations/github provider)
- 리포 설정, 브랜치 보호, Actions secrets를 코드화
- API 토큰(PAT) 관리 방식 결정 후 PoC

### Grafana Cloud (grafana/grafana provider)
- 데이터소스, 대시보드, 알림을 코드화
- 현재 OTLP 수집 중인 메트릭/로그/트레이스 기준 대시보드 정의
- Grafana Cloud API key 관리 방식 결정 후 PoC

## 3단계: state 관리 개선

- 로컬 state → 원격 state(S3 + DynamoDB lock) 도입 검토
- 외부 서비스용 state 분리 (external/github, external/grafana)

## 4단계: 관측성/운영

- ECS 헬스체크/오토스케일링 정책 정비
- 비용 모니터링 (프리티어 초과 알림)
