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

월 예상 요금

| 리소스 | 프리티어 | 온디멘드 + RI | 비고 |
| --- | --- | --- | --- |
| VPC·서브넷 | 무료 | 무료 | 과금 없음 |
| Cloud Map | 무료 | 무료 | 과금 없음 |
| VPC Link | 무료 | 무료 | 과금 없음 |
| Data Transfer | $0.23 | $0.23 | 아웃바운드 전송 |
| Route 53 | $1.01 | $1.01 | hosted zone + 쿼리 |
| API Gateway | ~$0 | ~$0 | 사용량 비례 |
| CloudFront | ~$0 | ~$0 | 사용량 비례 |
| VPC Public IPv4 주소 | $18.00 | $18.00 | 퍼블릭 IP 5개(Fargate 4 + RDS 1), $0.005/h |
| ECS Fargate SPOT | $12.28 | $12.28 | 태스크 4개(api-v1·api-v2·web-v2·worker), 0.25 vCPU / 0.5 GB |
| ElastiCache | $3.51 | $8.93 (RI) | Valkey, cache.t4g.micro |
| RDS MySQL | $0 | $13.50 (RI) | db.t4g.micro |
| OpenSearch | $0 | $40.88 (온디멘드) | t3.small.search, RI 불가 |
| Kinesis | $13.51 | $13.51 | yologram-post-view-event-prod, provisioned 1샤드 $0.0185/h (프리티어 없음) |
| DynamoDB | $0 | $0 | KCL 리스 테이블 1개(워커가 자동 생성, on-demand) — 항목 수개라 사실상 무과금 |
| Lightsail (n8n) | $0 (첫 3개월 무료) | $5 | 무료 종료 후 $5/월 |
| S3 | ~$0 | ~$0 | 사용량 비례 |
| SES | ~$0 | ~$0 | 사용량 비례 |
| ECR | $0.09 | $0.09 | 사용량 비례 |
| Google Workspace | $7.56 | $7.56 | Starter 플랜 |
| 합계($) | $56.19 | $120.99 | Google Workspace 포함 |
| 합계(₩) | 약 ₩84,000 | 약 ₩181,000 | ₩1,500/$ 기준 |


RI 요금

| 리소스 | 타입 | 온디멘드<br/>(시간당) | 온디멘드<br/>(월간) | 온디멘드<br/>(연간) | RI<br/>(시간당) | RI<br/>(월간) | RI<br/>(연간) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RDS MySQL | db.t4g.micro | $0.025 | $18.25 | $219 | $0.018 | $13.5 | $162.00 |
| ElastiCache (Valkey) | cache.t4g.micro | $0.0192 | $14.016 | $168.192 | $0.012237 | $8.93 | $107.20 |
| Opensearch | t3.small.search | $0.056 | $40.88 | $490.56 | - | - | - |

### 합계

(Google Workspace $7.56 포함, ₩1,500/$ 기준)

| 시나리오 | 월 | 연 |
| --- | --- | --- |
| 현재(프리티어) | 약 $56 (₩84,000) | 약 $674 (₩1,011,000) |
| 프리티어 만료 후(RI 적용) | 약 $121 (₩181,000) | 약 $1,452 (₩2,178,000) |
| 만료 후 + OpenSearch self-host($7) | 약 $88 (₩131,000) | 약 $1,050 (₩1,575,000) |

- 프리티어는 계정 생성 후 12개월 한정. 이후 만료 요금 적용
- 최대 절감 레버: OpenSearch 관리형($40.88) → Lightsail self-host($7)로 전환 시 만료 후 월 $34 절감