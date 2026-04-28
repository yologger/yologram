# n8n

워크플로우 자동화 플랫폼.

## 인프라

- AWS Lightsail Instance (Amazon Linux 2023)
- 인스턴스: micro_3_0 (1GB RAM, 2 vCPU, 40GB SSD)
- 고정 IP 할당 (인스턴스 연결 시 무료)
- 도메인: n8n.yologram.link (Route 53 A 레코드 -> 고정 IP)

## 앱 구성

- Docker Compose로 n8n + Caddy 실행 (/opt/n8n/)
- user_data로 인스턴스 생성 시 자동 프로비저닝 (Docker 설치, compose up)
- n8n: 워크플로우 엔진, 내장 SQLite, 데이터 /opt/n8n/data에 저장
- Caddy: 리버스 프록시, Let's Encrypt HTTPS 인증서 자동 발급/갱신

## 방화벽

- 80 (HTTP): 전체 오픈 (Caddy HTTPS 리다이렉트 + ACME 인증서 발급용)
- 443 (HTTPS): 전체 오픈
- SSH: Lightsail 브라우저 SSH로 접속 (22 포트 비개방)

## 요금

- 월 $5 (micro_3_0 기준)
    - 고정 IP: 인스턴스 연결 시 무료, 미연결 시 $0.005/hr
    - 데이터 전송: 월 2TB 포함, 초과 시 아웃바운드만 과금
    - 수동 스냅샷: $0.05/GB/mo

## 제약

- CPU burstable. 크레딧 소진 시 baseline 제한, 심한 경우 무응답
- 오토스케일링 없음 (단일 VM)
- 롤링 업데이트 미지원, docker-compose 재시작으로 업데이트
- 백업: 필요 시 수동 스냅샷

## 운영

```bash
# SSH 접속
Lightsail -> Instances -> n8n -> Connect using SSH

# 컨테이너 상태 확인
cd /opt/n8n && sudo docker-compose ps

# 로그 확인
sudo docker-compose logs -f

# https 에러 시 caddy 로그 확인
cd /opt/n8n && sudo docker-compose logs caddy --tail 10

# n8n 업데이트
sudo docker-compose pull && sudo docker-compose up -d

# Caddy 재시작 (인증서 재발급 등)
sudo docker-compose restart caddy
```
