# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

yologger 전용 n8n 워크플로우 자동화 플랫폼.

## 인프라

- IaC: Terraform (terraform/ 디렉토리)
- 클라우드: AWS Lightsail Instance (Amazon Linux 2023)
- AWS profile: yologram
- 리전: ap-northeast-2 (서울)
- 인스턴스: micro_3_0 (1GB RAM, 2 vCPU, 40GB SSD, 2TB 전송, $5/mo)
- n8n DB: 내장 SQLite

## Lightsail

- 요금: 월 $5, 고정 IP 무료 (미연결 시 과금), 스냅샷 $0.05/GB/mo
- 네트워크: 고정 IP, ALB 불필요, Route 53 도메인 연결 가능
- 보안: 자체 방화벽 (포트/IP 제한 가능), Geo 차단은 CloudFront + WAF 필요
- CPU: burstable 방식, 크레딧 소진 시 baseline 제한
- 오토스케일링 없음, 롤링 업데이트 미지원
- 백업: 필요 시 수동 스냅샷

## Terraform 명령어

```
cd terraform
terraform init      # 초기화
terraform plan      # 변경사항 확인
terraform apply     # 배포
terraform destroy   # 전체 삭제
```
