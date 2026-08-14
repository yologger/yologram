# AGENTS.md

yologram 모노레포의 terraform/ (AWS 인프라)에서 작업하는 에이전트를 위한 지침. CLAUDE.md는 이 파일을 가리키는 포인터다.
원래 별도 레포(yologram-infra)였고 2026-08 히스토리째 모노레포로 이관했다.

> 모노레포 구성·서비스 도메인·CI/CD는 루트 AGENTS.md, 디렉토리 트리와 요금은 README.md 참조.
> 이 파일은 terraform 고유 규칙(디렉토리별 리소스·명령어·패턴)만 둔다.

## 공통

- AWS profile: yologram
- 리전: ap-northeast-2 (서울)
- 디렉토리별 독립 state — aws/{global,services,tools}/{리소스}. state는 로컬 (각 디렉토리의 terraform.tfstate — gitignore. 디렉토리를 옮길 때는 git이 아니라 파일시스템으로 함께 옮겨야 리소스가 재생성되지 않는다)
- provider 캐시: ~/.terraformrc의 plugin_cache_dir로 공용화 — 지정하지 않으면 AWS provider(648MB)가 디렉토리마다 복사돼 18개 기준 11GB가 중복된다
- terraform apply는 사용자가 직접 실행. Claude는 코드 변경까지만 수행

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
- 관리형 도메인 코드 — 요금($40.88/월, RI 불가) 때문에 미생성. 셀프호스팅으로 대체 (아래 Lightsail)

### OpenSearch 셀프호스팅 (aws/global/lightsail/opensearch/)
- Lightsail small_3_0(2GB, Amazon Linux 2023) — OpenSearch + Dashboards + Caddy를 Docker Compose로 기동
- 도메인: opensearch.yologram.link / opensearch-dashboard.yologram.link (Route 53 A 레코드 + 정적 IP)
- HTTPS: Caddy가 Let's Encrypt 인증서 자동 발급·갱신
- 데이터: 인스턴스 디스크 바인드 마운트 (재시작·컨테이너 교체에도 유지) + 자동 스냅샷 일 1회(7일 롤링 고정, 기간 지정 불가)
- apply 시 admin_password 입력 필요 (tfvars에 두지 않음). user_data는 lifecycle ignore_changes — 변경해도 인스턴스를 재생성하지 않는다(재생성하면 데이터가 사라진다)
- 비밀번호는 compose가 아니라 env_file(os.env)로 주입 — compose가 `$`를 변수로 보간해 값이 잘린다
- nori 플러그인은 컨테이너 기동 시 설치 (공식 이미지 미포함, compose build는 buildx 0.17+ 요구라 불가)

### SQS (aws/global/sqs/)
- yologram-search-indexing-prod: 검색 인덱싱 작업 큐 (visibility 300s, long polling 20s) + DLQ(maxReceiveCount 3, 14일 보관)
- 큐는 대상별로 나누지 않는다 — 페이로드 target 필드로 분기 (docs/rules.md 「검색 인덱싱」)

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
- IAM: sqs-send 정책 — 검색 인덱싱 큐 SendMessage·GetQueueUrl만 (소비 권한은 주지 않는다)
- SSM(prod): Grafana OTLP (metrics/traces/logs) + DB writer/reader 접속정보 + JWT secret(유저·어드민) + Redis host(yologram.redis.host — ElastiCache valkey-prod, 커스텀 키인 이유: 로컬이 prod 파라미터 경로를 import하므로 spring.data.redis.host 그대로면 localhost를 덮어씀)
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
- ECS Fargate SPOT (0.5 vCPU, 1GB) — 0.25vCPU/512MB에서 Kinesis 컨슈머(KCL)의 Netty 이벤트 루프가 CPU를 얻지 못해 소비가 멈춘 선례로 상향
- Spring Boot 비동기 워커, 인바운드 트래픽 없음 (API Gateway·Cloud Map·portMappings 미사용, SG는 egress만)
- ECR: yologram-worker
- SSM(prod): Grafana OTLP (metrics/traces/logs) + DB writer/reader 접속정보 + LLM API 키(yologram.llm.gemini/groq.api-key) + Discord 웹훅 채널별 url(yologram.webhooks.discord.{tech,invest,politics}-news.url — enabled는 yaml) + cache.data.redis.host + OpenSearch 접속(opensearch.main.{enabled,uri,username,password} — enabled·uri는 tf가 실제 값 관리, 자격증명만 PLACEHOLDER+ignore_changes)
- IAM: sqs-receive 정책 — 인덱싱 큐 ReceiveMessage·DeleteMessage·ChangeMessageVisibility·GetQueueUrl/Attributes (발행 권한은 주지 않는다)
- IAM: kinesis-get 정책 — 조회 이벤트 스트림 읽기(DescribeStream·DescribeStreamSummary·GetRecords·GetShardIterator·ListShards) + ListStreams + KCL 리스 테이블(yologram-post-view-event-lease-prod) CRUD·CreateTable. 리스 테이블은 tf가 아니라 KCL이 부팅 시 자동 생성한다(PAY_PER_REQUEST) — 지우면 체크포인트가 사라져 그 사이 이벤트가 유실된다
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
