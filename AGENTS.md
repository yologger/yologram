# yologram-v1 프로젝트 지침 (Codex)

## 프로젝트 구조

- 모노레포 구성
- api/: Spring Boot (MVC/WebFlux 미정)
- web/: React

## 배포

- web: S3 배포
- api: AWS App Runner (고려 중)

## CI/CD

- GitHub Actions 사용
- api/ 변경 시 API 전용 workflow 트리거
- web/ 변경 시 Web 전용 workflow 트리거

## 작업 규칙

- 코드 변경 전 반드시 plan을 먼저 보여주고, 승인 후 적용
- 코드 변경 시 README.md, CLAUDE.md, AGENTS.md를 함께 업데이트
