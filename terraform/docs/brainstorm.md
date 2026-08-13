# yologram-infra 브레인스토밍

## 목적

- yologram 전체 AWS 인프라를 Terraform으로 코드화하여 관리
- 환경별/서비스별 리소스를 독립 state로 분리

## 관리 원칙

- 도구: Terraform (HCL)
- AWS profile: yologram, 리전: ap-northeast-2 (서울)
- state: 로컬 (현재). 협업/이력 필요 시 원격 state 검토
- terraform apply는 사용자가 직접 실행. 코드 변경까지만 자동화
- 시크릿(비밀번호, 토큰)은 코드/state에 평문 저장 금지. variable 입력 또는 SSM SecureString 사용

## 디렉토리 전략

- aws/global/ - 환경 공통 리소스 (VPC, ECS, API Gateway, Database, ElastiCache, OpenSearch, IAM)
- aws/services/ - 개별 서비스 인프라 (n8n, api-v1/v2, web-v1/v2)
- 각 디렉토리가 독립된 state를 가짐 → blast radius 축소, 개별 apply 가능

## Data Source 패턴

- VPC/서브넷은 하드코딩 대신 tag 필터 data source로 참조
- 계정 ID는 data.aws_caller_identity.current.account_id 사용 (하드코딩 금지)

## 보안

- public repository 전환 고려 → 자격증명/계정 ID 노출 점검 완료
- 계정 ID는 git 히스토리에서도 제거 (filter-repo)
- IAM은 최소권한 지향 (현재 github-actions-deployer 과잉권한은 개선 대상)

## IaC 확장 검토 대상

AWS 외 외부 서비스도 Terraform으로 편입 가능한지 검토.

### GitHub
- provider: integrations/github (공식)
- 관리 가능: 리포지토리, 브랜치 보호 규칙, Actions secrets/variables, 팀/협업자, 웹훅
- 활용: 리포 설정과 Actions 시크릿(예: AWS 배포 자격증명)을 코드화

### Grafana Cloud
- provider: grafana/grafana (공식)
- 관리 가능: Cloud Stack, 데이터소스, 대시보드, 알림(alerting), Contact Point, OnCall
- 활용: 현재 각 서비스가 OTLP로 Grafana Cloud에 전송 중 → 대시보드/알림을 코드화

### 결론
- 둘 다 공식 provider 존재, Terraform 관리 가능
- 단 API 토큰(GitHub PAT, Grafana Cloud API key) 관리 방식이 선행 과제
- 별도 state(aws/ 밖, 예: external/github, external/grafana)로 분리 권장
