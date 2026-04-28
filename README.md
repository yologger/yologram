# yologger-n8n

yologger 전용 n8n 워크플로우 자동화 플랫폼.

## 인프라

- IaC: Terraform
- 클라우드: AWS Lightsail (~~container~~ instance)
- 리전: ap-northeast-2 (서울)
- 인스턴스: OS: Amazon Linux 2023, size: micro_3_0 (1GB RAM, 2 vCPU, 40GB SSD)
- n8n DB: 내장 SQLite

## AWS Lightsail

- 요금: 월 $5 (micro_3_0 기준)
    - 고정 IP: 인스턴스 연결 시 무료, 미연결 시 $0.005/hr
    - 데이터 전송: 월 2TB 포함, 초과 시 아웃바운드만 과금
    - 수동 스냅샷: $0.05/GB/mo
- 네트워크:
    - 고정 IP 무료 할당, ALB 불필요
    - Route 53 도메인 연결 가능 (A 레코드 → 고정 IP)
- 보안:
    - 자체 방화벽 제공 (EC2 Security Group과 별도)
    - 포트/프로토콜/소스 IP 기반 접근 제한 가능 (CIDR, 개별 IP)
    - Geo 차단: Lightsail 자체로는 불가, CloudFront + WAF 필요
- CPU: burstable 방식. 크레딧 소진 시 baseline으로 제한 (재시작 아님). 심한 경우 무응답 → 수동 재시작 필요
- 오토스케일링: 없음 (단일 VM)
- 롤링 업데이트: 미지원, Docker Compose 컨테이너 재시작으로 업데이트
- 백업: 필요 시 수동 스냅샷 생성

## Terraform

```bashte
cd terraform
terraform init      # 초기화
terraform plan      # 변경사항 확인
terraform apply     # 배포
terraform destroy   # 전체 삭제
```
