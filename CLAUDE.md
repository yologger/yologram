# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

yologger AWS 인프라 관리. Terraform으로 환경별/서비스별 리소스를 분리 관리.

## 구조

- prod/common/ - 환경 공통 리소스 (ECS 클러스터, API Gateway, Database)
- prod/service/ - 개별 서비스 인프라 (n8n, yologram-api-v1 등)
- global/ - 환경 무관 공유 리소스 (IAM)
- 각 디렉토리가 독립된 terraform state를 가짐

## 공통

- AWS profile: yologram
- 리전: ap-northeast-2 (서울)
- state: 로컬

## Service

## n8n (prod/service/n8n/)

- AWS Lightsail Instance (Amazon Linux 2023, micro_3_0, $5/mo)
- Docker Compose로 n8n + Caddy 구성 (user_data로 자동 프로비저닝)
- 도메인: n8n.yologram.link (Route 53 A 레코드)
- HTTPS: Caddy가 Let's Encrypt 인증서 자동 발급/갱신
- n8n DB: 내장 SQLite (/opt/n8n/data)
- 방화벽: 80(HTTP), 443(HTTPS)만 개방. SSH는 Lightsail 브라우저 접속

## yologram-api-v1 (prod/service/yologram-api-v1/)

- ECS Fargate SPOT (0.25 vCPU, 512MB)
- Spring Boot, 컨테이너 포트 8080
- ECR: yologram-api-v1
- API Gateway 경로: api.yologram.link/api/v1/*
- Cloud Map 서비스 디스커버리로 API Gateway 연결

## Terraform 명령어

```
cd prod/service/n8n    # 각 디렉토리에서 개별 실행
terraform init
terraform plan
terraform apply
terraform destroy
```
