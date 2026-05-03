# yologram-api-v2 구현 계획

## Phase 1: 프로젝트 초기 구성

1. uv init으로 프로젝트 생성
2. 의존성 추가: fastapi, uvicorn, pydantic-settings
3. 디렉토리 구조 생성 (app/config, app/domain, app/core)
4. .env, .env.prod, .env.staging 파일 생성
5. .gitignore 설정 (.env 파일 제외 정책 결정)

## Phase 2: 설정 및 DI 기반 구성

1. app/config/settings.py - pydantic BaseSettings 기반 설정 클래스
2. app/core/di.py - Settings 인스턴스 제공 팩토리 함수
3. app/main.py - FastAPI 앱 생성 및 라우터 등록

## Phase 3: Test 도메인 구현 (v1 대응)

1. app/domain/test/router.py - /api/v2/test 엔드포인트
   - GET / : 기본 응답
   - GET /echo : 클라이언트 요청 정보 반환
   - GET /profile : 활성 프로파일 반환
   - GET /property?key=... : 설정값 조회
2. app/domain/test/service.py - 비즈니스 로직 (설정 조회 등)
3. app/domain/test/schema.py - 요청/응답 스키마

## Phase 4: 컨테이너화

1. Dockerfile 작성 (uv 기반 빌드)
2. .dockerignore 작성
3. GitHub Actions 워크플로우 내용 채우기 (yologram-api-v2.yaml)

## Phase 5: 인프라 (yologger-infra에서)

1. ECR 리포지토리 생성
2. ECS Task Definition + Service 생성
3. Secret은 Parameter Store + ECS Task Definition secrets로 주입
