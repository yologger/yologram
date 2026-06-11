# yologram

모노레포 구성의 풀스택 프로젝트. 동일 로직을 다른 기술 스택으로 구현.

## 구조

- yologram-api-v1/: Spring Boot MVC (Kotlin)
- yologram-web-v1/: React
- yologram-api-v2/: FastAPI
- yologram-web-v2/: Next.js
- .github/workflows/: GitHub Actions

## 인프라

- [https://github.com/yologger/yologram-infra](https://github.com/yologger/yologram-infra)
- IaC: Terraform (yologram-infra 레포에서 관리)
- ECS Fargate: api-v1(5000), api-v2(5000), web-v2(3000)
- API Gateway: api.yologram.link / web.v2.yologram.link → /api/v1/{proxy+}는 api-v1, /api/v2/{proxy+}는 api-v2, /{proxy+}는 web-v2
- web-v1: S3 + CloudFront (web.v1.yologram.link)


```mermaid
flowchart LR
    Client([Client])

    Client -- "web.v1.yologram.link" --> CloudFront
    Client -- "api.yologram.link<br/>web.v2.yologram.link" --> APIGW

    subgraph AWS
        CloudFront --> S3["S3<br/>yologram-web-v1"]

        APIGW["API Gateway<br/>yologram-gateway (HTTP API)"]
        APIGW -- "/api/v1/*" --> ECS_API_V1["ECS Fargate<br/>yologram-api-v1<br/>:5000"]
        APIGW -- "/api/v2/*" --> ECS_API_V2["ECS Fargate<br/>yologram-api-v2<br/>:5000"]
        APIGW -- "/* (catch-all)" --> ECS_WEB_V2["ECS Fargate<br/>yologram-web-v2<br/>:3000"]

        ECS_API_V1 --> RDS[(RDS MySQL)]
        ECS_API_V2 --> RDS
    end
```

> 초록색 경로: web.v2.yologram.link(서브도메인)로 들어온 요청이 catch-all(/*) route를 통해 web-v2에 도달.
> 두 커스텀 도메인(api/web.v2)은 동일 게이트웨이/스테이지($default)를 공유하며, 분기 자체는 경로(route_key) 기반.
> API Gateway → ECS 연결은 VPC Link + Cloud Map(service discovery) 경유.
> ElastiCache(Valkey)와 OpenSearch는 프로비저닝되어 있으나 앱 연결은 별도 (다이어그램 생략).

## CI/CD

- GitHub Actions (디렉토리별 독립 트리거)
- ECR push 시 이미지 태그: {branch}-{commit SHA 8자리}
- 배포 결과 Discord 웹훅 알림

## ECS 컨테이너 접속

```bash
aws ecs execute-command \
  --cluster ecs-prod \
  --task <task-id> \
  --container yologram-api-v1 \
  --interactive \
  --command "/bin/sh" \
  --profile yologram \
  --region ap-northeast-2
```
