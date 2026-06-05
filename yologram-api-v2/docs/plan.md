# yologram-api-v2 구현 계획

## Phase 1: 프로젝트 초기 구성 (완료)

1. uv init으로 프로젝트 생성
2. 의존성 추가: fastapi, uvicorn, pydantic-settings
3. 디렉토리 구조 생성 (app/config, app/domain, app/core)
4. .env, .env.prod, .env.staging 파일 생성

## Phase 2: 설정 및 DI 기반 구성 (완료)

1. app/config/settings.py - pydantic BaseSettings 기반 설정 클래스
2. app/main.py - FastAPI 앱 생성 및 라우터 등록

## Phase 3: Test 도메인 구현 (완료)

1. app/domain/test/router.py - /api/v2/test 엔드포인트
2. app/domain/test/service.py - 비즈니스 로직
3. app/domain/test/schema.py - 요청/응답 스키마

## Phase 4: 컨테이너화 (완료)

1. Dockerfile 작성 (uv 기반 빌드)
2. GitHub Actions 워크플로우

## Phase 5: Observability (완료)

1. Grafana Cloud OTLP - Logs, Traces, Metrics

## Phase 6: UMS 회원가입

### 의존성
- sqlalchemy, pymysql (ORM + MySQL)
- bcrypt (비밀번호 해싱)
- pytest, httpx (테스트)

### DB 설정
- Settings에 db_url, db_username, db_password 추가
- SQLAlchemy engine + SessionLocal
- get_db 의존성 함수

### 공통
- ApiEnvelop 응답 래퍼 ({ "data": T })
- 예외 처리 (UserDuplicateException → 409)
- CORS 전체 허용

### UMS 도메인
- model: User (id, email, name, nickname, password, avatar, access_token, type, status, dates)
- enum: UserType, UserStatus
- schema: JoinRequest, JoinResponse
- repository: UserRepository
- service: UserService.join()
- router: POST /api/v2/ums/user/join

### 테스트
- service 단위 테스트
- router 테스트 (TestClient)
