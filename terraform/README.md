# terraform

yologram AWS 인프라 (Terraform). 디렉토리별로 독립된 state를 가진다.

> 서비스 도메인·아키텍처 다이어그램은 [루트 README](../README.md), 작업 규칙은 [AGENTS.md](AGENTS.md) 참조.

## 구조

```
aws/
  global/
    vpc/                    # VPC (vpc-prod, 10.0.0.0/16) + 서브넷 pub-a·pub-b
    ecs/                    # ECS 클러스터 (ecs-prod, Fargate SPOT) + Cloud Map + ecs-task-execution-role
    iam/                    # GitHub Actions 배포 유저
    api-gateway/            # HTTP API Gateway (yologram-gateway), VPC Link, Custom Domain
    database/               # RDS MySQL 8.0 (mysql-prod, db.t4g.micro)
    elasticache/            # Valkey 8.0 (valkey-prod, cache.t4g.micro)
    ses/                    # SES 도메인 인증 (no-reply@yologram.link)
    kinesis/                # Kinesis 스트림 (yologram-post-view-event-prod — 게시글 조회 이벤트, 1샤드)
    dynamodb/               # (테이블 정의 없음 — KCL 리스 테이블은 워커가 자동 생성, state·provider만 잔존)
    opensearch/             # OpenSearch 관리형 — 코드만 있고 도메인 미생성(요금 때문에 self-host로 대체)
    lightsail/opensearch/   # OpenSearch 셀프호스팅 (small_3_0 + Dashboards + Caddy, opensearch.yologram.link)
    sqs/                    # SQS (yologram-search-indexing-prod — 검색 인덱싱 작업 + DLQ)
  services/
    yologram-api-v1/        # Spring Boot API (ECS Fargate SPOT)
    yologram-api-v2/        # FastAPI (ECS Fargate SPOT)
    yologram-web-v1/        # S3 + CloudFront (SPA)
    yologram-web-v2/        # Next.js (ECS Fargate SPOT)
    yologram-worker/        # Spring Boot 비동기 워커 (ECS Fargate SPOT 0.5vCPU/1GB, 인바운드 없음)
    yologram-admin-web/     # 어드민 웹, S3 + CloudFront (SPA)
  tools/
    n8n/                    # n8n 워크플로우 자동화 (Lightsail) — 제거 예정, 리소스 없음
    yologger-blog/          # 기술 블로그 (S3 + CloudFront, blog.yologram.link)
```

## 요금

월 요금 (Cost Explorer 실측. 2026-08-21~29 9일치 실사용료 $33.58을 월 환산한 값이 $113.42로, 아래 항목 합계와 오차 0.5% 이내로 일치한다)

| 리소스 | 월 | 비고 |
| --- | --- | --- |
| VPC·서브넷 / Cloud Map / VPC Link | 무료 | 과금 없음 |
| VPC 퍼블릭 IPv4 주소 | $18.25 | IP 5개(Fargate 4 + RDS 1) × $0.005/h. 2024-02부터 모든 퍼블릭 IP 과금 |
| RDS MySQL db.t4g.micro | $18.25 | 온디맨드 $0.025/h |
| RDS 스토리지·스냅샷 | $2.73 | gp2 20GB × $0.131/GB-월 |
| ECS Fargate SPOT | $21.76 | 태스크 4개 — api-v1·v2 0.5vCPU/1GB, web-v2 0.25/0.5, worker 0.5/1. Spot 단가는 온디맨드의 30% |
| Lightsail (OpenSearch self-host) | $24.00 | medium_3_0 4GB/2vCPU/80GB. 관리형 t3.small.search($40.88) 대체 |
| ElastiCache | $14.02 | Valkey cache.t4g.micro $0.0192/h |
| Kinesis | $13.50 | provisioned 1샤드 $0.0185/h (프리티어 없음) |
| Route 53 | $1.00 | hosted zone 2개 |
| ECR | $0.23 | $0.10/GB-월 × 약 1.5GB |
| DynamoDB | ~$0 | KCL 리스 테이블 1개(on-demand, 항목 수개) |
| API Gateway / CloudFront / S3 / SES / Data Transfer | ~$0.5 | 사용량 비례 |
| **AWS 합계** | **$114** | |
| Google Workspace | $7.56 | Starter 플랜 |
| **총 합계** | **$122** | 약 ₩183,000 (₩1,500/$ 기준) |

### 요금 변화 (2026-08-20)

| 시점 | 월 | 연 |
| --- | --- | --- |
| MySQL 8.0 + 기존 스펙 | 약 $271 | 약 $3,252 |
| MySQL 8.4 업그레이드 후 | 약 $96 | 약 $1,152 |
| + api-v1·v2·OpenSearch 스케일업 (현재) | 약 $114 | 약 $1,368 |

- **MySQL 8.0 Extended Support가 월 $175였다** — 2 vCPU × $0.12/vCPU-h로, 인스턴스 요금($18)의 10배이자 전체의 65%였다. 8.4 LTS로 올려 제거했다. 8.0 계열은 마이너를 올려도 이 요금이 붙는다
- 스케일업(+$18)은 그 절감분 안에서 처리된다 — api-v1·v2 0.25→0.5 vCPU가 +$6.22, OpenSearch 2GB→4GB가 +$12.23
- 조치 결과는 청구로 확인됐다(2026-08-30 기준): Extended Support가 8/21부터 9일 연속 $0, 전체 월 환산 $113.42
- 현재 청구는 크레딧으로 전액 상쇄되고 있다(7월부터). 위 금액은 크레딧 소진 후 실제로 나갈 금액이고, 잔액·만료일은 Billing 콘솔 → Credits에서 확인한다
- 트래픽이 늘어도 요금은 거의 그대로다 — 대부분이 시간당 고정(인스턴스·퍼블릭 IP·샤드)이고, RDS 스토리지도 20GB 중 18GB가 남아 있다
- 퍼블릭 IPv4($18)를 줄이려면 Fargate를 프라이빗 서브넷으로 옮겨야 하는데 NAT Gateway가 월 $43라 오히려 3배 비싸진다 — 현 구성이 최적이다
- OpenSearch 관리형($40.88) → Lightsail self-host 전환은 이미 반영돼 있다
