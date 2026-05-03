# yologram-api-v1

Spring Boot API 서비스.

## 아키텍처

```
사용자
  │ https://api.yologram.link/api/v1/*
  ▼
Route 53 (DNS)
  │ api.yologram.link → d-xxx.execute-api (ALIAS)
  ▼
API Gateway (Custom Domain)
  │ api.yologram.link → yologram-gateway API 매핑
  ▼
API Gateway (HTTP API: yologram-gateway)
  │ Route: ANY /api/v1/{proxy+}
  │ Stage: $default (스로틀링 3 req/sec)
  ▼
VPC Link
  │ API Gateway → VPC 내부 연결
  ▼
Cloud Map (서비스 디스커버리)
  │ yologram-api-v1.ecs-prod.internal → 현재 태스크 IP:8080
  ▼
ECS Fargate Task (Spring Boot)
  │ 컨테이너 포트 8080
  ▼
응답 반환
```

## AWS 리소스

### Route 53
DNS. api.yologram.link → API Gateway 커스텀 도메인 엔드포인트로 해석

### API Gateway: 
HTTPS 처리, 경로 라우팅 (/v1/*), 스로틀링 (악성 트래픽 방지)
- VPC Link: 
    - API Gateway가 VPC 내부로 들어가는 문. VPC 내부 리소스(ECS, EC2, EKS 등)에 접근하려면 필수. 
    - 대상이 public IP를 가지고 있으면 VPC Link 없이 직접 호출 가능. 근데 그러면 API Gateway를 쓰는 의미가 줄어듦.
- Custom Domains
### Cloud Map
Fargate 태스크의 전화번호부. 태스크 IP가 바뀔 때마다 자동 업데이트하여 현재 태스크를 찾을 수 있게 함
- Namespace 1개 → Route 53 Private Hosted Zone 1개 생성 (ecs-prod.internal)
- Service 1개 → Hosted Zone 안에 DNS 레코드 1개 생성 (yologram-api-v1.ecs-prod.internal)
- 서비스 추가 시 같은 네임스페이스에 레코드만 추가됨 (예: yologram-api-v2.ecs-prod.internal)

### ECS
- Cluster → Service → Task: Cloud Map에 태스크 IP 자동 등록/해제 (service_registries 설정)
- 태스크 시작 시 Cloud Map에 IP 등록, 종료 시 자동 해제

## 리소스 생성 순서

### 1단계: 공통 인프라 (prod/common/)

```bash
# ECS 클러스터 + Task Execution Role
cd prod/common/ecs
terraform apply

# API Gateway + VPC Link + Cloud Map + 커스텀 도메인
cd prod/common/api-gateway
terraform apply
```

생성되는 리소스:
- ECS 클러스터 (ecs-prod, Fargate SPOT)
- IAM Role (ecs-task-execution-role)
- Cloud Map 네임스페이스 (ecs-prod.internal)
- API Gateway HTTP API (yologram-gateway)
- API Gateway Stage ($default)
- VPC Link + 보안그룹
- ACM 인증서 (api.yologram.link)
- API Gateway 커스텀 도메인 + API 매핑
- Route 53 A 레코드 (api.yologram.link → 커스텀 도메인)

### 2단계: 서비스 (prod/service/yologram-api-v1/)

```bash
cd prod/service/yologram-api-v1
terraform apply
```

생성되는 리소스:
- ECR 리포지토리 (yologram-api-v1) + lifecycle policy (최신 5개 유지)
- IAM Role (yologram-api-v1-prod-role) + SSM 권한
- 보안그룹 (8080 포트)
- ECS Task Definition (0.25 vCPU, 512MB)
- Cloud Map 서비스 (yologram-api-v1)
- ECS Service (Cloud Map 서비스 디스커버리 연결)
- API Gateway Integration + Route (ANY /api/v1/{proxy+})

### 3단계: 배포

```bash
# ECR에 이미지 push 후 ECS 서비스 업데이트
aws ecs update-service --cluster ecs-prod --service yologram-api-v1-prod --force-new-deployment --profile yologram --region ap-northeast-2
```

## 인프라 사양

- ECS Fargate SPOT (0.25 vCPU, 512MB)
- 컨테이너 포트: 8080
- ECR: yologram-api-v1
- ECS Exec: 활성화

## 운영

```bash
# 태스크 목록
aws ecs list-tasks --cluster ecs-prod --service-name yologram-api-v1-prod --profile yologram --region ap-northeast-2

# 컨테이너 접속
aws ecs execute-command --cluster ecs-prod--task <task-id> --container yologram-api-v1 --interactive --command "/bin/sh" --profile yologram --region ap-northeast-2

# 강제 재배포
aws ecs update-service --cluster ecs-prod --service yologram-api-v1-prod --force-new-deployment --profile yologram --region ap-northeast-2
```
