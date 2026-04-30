# service-prod

서비스 공용 MySQL 데이터베이스.

## 인프라

- RDS MySQL 8.0.42
- 인스턴스: db.t4g.micro
- 스토리지: 20GB gp2, 암호화 활성 (KMS)
- AZ: ap-northeast-2b
- Multi-AZ: 비활성
- 퍼블릭 액세스: 활성

## 접속 정보

- Endpoint: serivce-prod.cv4imowma8xe.ap-northeast-2.rds.amazonaws.com
- Port: 3306
- Username: admin

## 보안그룹

- 이름: db-service-prod-sg
- 3306 포트만 허용, 접속 IP 제한 (VPC 내부 + 허용된 개인 IP만)

## 파라미터 그룹

- 이름: service-prod-mysql80
- max_connections: 1000
- wait_timeout: 28800

## 백업

- 자동 백업: 비활성 (retention 0)
- 필요 시 수동 스냅샷 생성

## Deletion Protection

- 비활성 상태. 프로덕션 운영 시 활성화 권장
