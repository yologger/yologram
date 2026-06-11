## 인프라 구조

- [x] aws/ 디렉토리로 인프라 이전 (global/, services/ 분리)
- [x] 기존 prod/, global/ 디렉토리 제거
- [x] n8n을 aws/services/n8n으로 이전
- [x] 서비스별 독립 state 분리

## 보안

- [x] 평문 자격증명/키 노출 점검 (현재 트리 + git 히스토리)
- [x] 계정 ID 하드코딩 제거 (data.aws_caller_identity 치환)
- [x] git 히스토리에서 계정 ID 제거 (filter-repo)
- [x] *.tfvars gitignore 추가 + 기존 추적 해제
- [ ] IAM github-actions-deployer 최소권한화 (AmazonS3FullAccess 축소)
- [ ] GitHub Actions 배포 자격증명 IAM User → OIDC 전환 검토

## IaC 확장 검토

- [ ] GitHub를 Terraform(integrations/github provider)으로 관리 가능한지 검토
  - 리포 설정, 브랜치 보호, Actions secrets/variables 코드화 범위 확인
  - PAT 등 인증 토큰 관리 방식 결정
- [ ] Grafana Cloud를 Terraform(grafana/grafana provider)으로 관리 가능한지 검토
  - 데이터소스/대시보드/알림 코드화 범위 확인
  - Grafana Cloud API key 관리 방식 결정

## state 관리

- [ ] 원격 state(S3 + DynamoDB lock) 도입 검토
- [ ] 외부 서비스용 state 분리 (external/github, external/grafana)

## 운영

- [ ] ECS 헬스체크 설정
- [ ] 비용 모니터링 (프리티어 초과 알림)

## 문서

- [x] CLAUDE.md / AGENT.md / README.md 최신화
- [x] docs/ (brainstorm, plan, tasks) 구성
