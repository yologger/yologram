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

## Phase 8: 회원정보 조회

### API
- GET /api/v2/ums/user/me (본인 정보 조회, 인증 필요)

### 테스트
- user_service 단위 테스트
- user_router E2E 테스트

## Phase 9: 회원정보 수정

### API
- PATCH /api/v2/ums/user/me (이름, 닉네임 변경, 인증 필요)

### 테스트
- user_service 단위 테스트
- user_router E2E 테스트

## Phase 10: 비밀번호 변경

### API
- PATCH /api/v2/ums/user/me/password (현재 비밀번호 + 새 비밀번호, 인증 필요)

### 테스트
- 비밀번호 변경 서비스 단위 테스트
- 비밀번호 변경 E2E 테스트

## Phase 11: 이메일 인증 (AWS SES)

### 흐름
- 회원가입 시 이메일로 인증 코드 발송 (AWS SES)
- 사용자가 인증 코드 입력 → 검증 통과 후 가입 완료
- email_verification_codes 테이블에 코드 저장 (5분 만료)

### API
- POST /api/v2/ums/auth/email-verification/send
- POST /api/v2/ums/auth/email-verification/verify

## Phase 12: 비밀번호 찾기 (AWS SES)

### 흐름
- 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (회원가입 이메일 인증과 동일 패턴, api-v1과 동일)
- 저장: password_reset_codes 테이블 (5분 만료), confirm 시 코드 재검증 후 변경·삭제

### API
- POST /api/v2/ums/auth/password-reset/send (미가입 시 404, 코드 발송)
- POST /api/v2/ums/auth/password-reset/verify (코드 검증 → verified)
- POST /api/v2/ums/auth/password-reset/confirm (email, code, newPassword → 재검증 후 변경)

### 예외
- UserNotFoundException (404): 미가입 이메일
- PasswordResetExpiredException (400): 코드 만료
- PasswordResetInvalidException (400): 코드 불일치

## 회원탈퇴

### 현재 (개발 단계: 하드 삭제)
- DELETE /api/v2/ums/user/me: 유저 레코드를 즉시 삭제 → email 해제로 재가입 가능 (api-v1과 동일)

### 추후 (b 방식: soft delete + 유예 후 정리)
- status=DELETED + deleted_date, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403)
- 유예기간 후 PII 익명화/하드삭제 배치, 연관 데이터 비동기 정리, 조회 시 DELETED 필터링, email 재가입 정책

## Phase 13: Refresh Token

### 흐름
- login 시 access token + refresh token 쌍 발급
- access token 만료 시 refresh token으로 재발급

### API
- POST /api/v2/ums/auth/refresh

## Phase 7: UMS 로그인/로그아웃/토큰검증

### 설정
- Settings에 jwt_secret (환경변수 JWT_SECRET), jwt_expire(86400), jwt_issuer(yologram.link), jwt_audience(yologram.client) 추가
- run-prod.sh에 JWT_SECRET Parameter Store 가져오기 추가

### JWT
- PyJWT 의존성 추가
- jwt_util.py: create_token(uid), validate_and_get_uid(token)
- api-v1과 동일한 HMAC256, 동일한 secret/issuer/audience

### 인증 흐름
- auth_schema.py: LoginRequest, LoginResponse, ValidateTokenResponse, AuthData
- auth_dependency.py: get_authenticated_user (Bearer 헤더 → JWT 검증 → AuthData)
- auth_service.py: login(비밀번호 검증→토큰 생성→DB 저장), validate_token(DB 일치 확인), logout(access_token null)
- auth_router.py: login, validate-token, logout 엔드포인트
- 예외: AuthWrongPasswordException(401), AuthTokenExpiredException(401), AuthTokenInvalidException(401)

### 테스트
- jwt_util 단위 테스트
- auth_service 단위 테스트 (mock repository)
- auth_router E2E 테스트 (TestClient)

## CMS: 커뮤니티 카테고리 (api-v1 미러링)

### 도메인/스키마
- app/domain/cms: Section enum(TECH/INVEST/POLITICS), Category 모델, CategoryRepository, CategoryService, router
- categories 테이블 api-v1과 DB 공유 (id, section, name, sort_order, is_active, created_at)

### API
- GET /api/v2/cms/{section}/categories → is_active=true, sort_order 정렬, 응답 { id, name, sortOrder }
- 잘못된 section → 400 INVALID_SECTION (InvalidSectionException, core/exception.py)

### 테스트
- CategoryService 단위 테스트 (mock repository)
- cms_router E2E 테스트 (TestClient)

## PMS: 커뮤니티 게시글 작성 (api-v1 미러링)

### 도메인/스키마
- app/domain/pms: Post / PostCategory 모델, PostRepository / PostCategoryRepository, PostService, router
- CategoryQueryClient(Protocol) + LocalCategoryQueryClient: cms 카테고리 검증 경계 추상화 (MSA 분리 대비)
- community_posts / post_categories 테이블 api-v1과 DB 공유, 경계 넘는 참조는 FK 없이 인덱스

### API
- POST /api/v2/pms/{section}/posts (인증 필요), 요청 { title?, content, categoryIds[] }, 응답 { id } (201)
- 작성자=인증 유저, categoryIds 1~3개 필수 + section 일치 검증
- 예외: 400 INVALID_CATEGORY / INVALID_SECTION / VALIDATION_ERROR

### 검증 응답 통일 (api-v1 정합)
- RequestValidationError 핸들러: status 422 → 400, errorCode VALIDATION_ERROR, 메시지 단일 문자열화
- 기존 ums/auth 검증 응답도 400으로 통일

### 테스트
- PostService 단위 테스트 (mock repository/client)
- pms_router E2E 테스트 (TestClient)
