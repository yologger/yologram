# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

yologger AWS 인프라 관리. Terraform으로 서비스별/글로벌 리소스를 분리 관리.

## 구조

- terraform/service/ - 개별 서비스 인프라 (n8n 등)
- terraform/global/ - 공유 리소스 (IAM, Route 53, Database)
- 각 디렉토리가 독립된 terraform state를 가짐

## 공통

- AWS profile: yologram
- 리전: ap-northeast-2 (서울)
- state: 로컬

## Service

## n8n (terraform/service/n8n/)

- AWS Lightsail Instance (Amazon Linux 2023)
- 인스턴스: micro_3_0 (1GB RAM, 2 vCPU, 40GB SSD, $5/mo)
- n8n DB: 내장 SQLite
- 네트워크: 고정 IP, ALB 불필요, Route 53 도메인 연결 가능
- 보안: 자체 방화벽 (포트/IP 제한 가능), Geo 차단은 CloudFront + WAF 필요
- CPU burstable, 오토스케일링 없음, 롤링 업데이트 미지원

## Terraform 명령어

```
cd terraform/service/n8n    # 각 디렉토리에서 개별 실행
terraform init
terraform plan
terraform apply
terraform destroy
```
