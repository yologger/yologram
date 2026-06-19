# yologram-api-v2 에이전트 가이드

## 프로젝트 개요

FastAPI 기반 API 서버. ECS Fargate에서 운영.

## 주요 파일

- app/main.py: 앱 진입점 (logging, metrics, tracing 초기화)
- app/config/settings.py: Pydantic Settings (환경변수 매핑)
- app/config/logging.py: OTLP 로그 설정
- app/config/metrics.py: OTLP 메트릭 설정
- app/config/tracing.py: OTLP 트레이스 설정

## 코드 컨벤션

- 의존성 관리: uv (pyproject.toml + uv.lock)
- 설정: pydantic-settings (환경변수 자동 매핑)
- 로깅: Python logging + OpenTelemetry LoggingHandler

## API / 예외

- 응답 래퍼: ApiEnvelop ({ "data": T })
- 예외: AppException → { "errorMessage", "errorCode" }
- 입력값 검증 실패: 400 VALIDATION_ERROR, 메시지는 단일 문자열 (status·errorCode api-v1 정합)
- 라우팅 예외도 동일 형식: 404 → NOT_FOUND, 405 → METHOD_NOT_ALLOWED (StarletteHTTPException 핸들러)
- CORS: 전체 허용 (*)
- Swagger: /api/v2/docs

## 이메일 인증

- EmailSender 프로토콜로 발송 추상화
- StubEmailSender: 로그 출력 (app_profile != prod, 개발/테스트용)
- SesEmailSender: AWS SES 발송 (app_profile == prod, boto3)
- get_email_sender 의존성으로 프로파일에 따라 주입
- 발신 주소: no-reply@yologram.link (ses_from_address 설정)
- 리전: ap-northeast-2
- 자격증명: ECS Task Role (prod), AWS_PROFILE 환경변수 (로컬, scripts/run-prod.sh)
- EmailVerificationCode 모델: email, code(6자리), verified, expired_at(5분), created_at / 테이블 email_verification_codes
- 엔드포인트: POST /api/v2/ums/auth/email-verification/send, /verify
- 회원가입 시 이메일 인증 필수 (UserService.join에서 verified 확인, 가입 후 코드 삭제)

## 비밀번호 찾기

- 방식: 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (이메일 인증과 동일 패턴/SES 재사용, api-v1과 동일)
- 저장: 별도 테이블 password_reset_codes (PasswordResetCode 모델: email, code, verified, expired_at 5분, created_at) — api-v1과 공유
- PasswordResetService: send_code(미가입 시 UserNotFoundException 404, 기존 코드 삭제 후 발송), verify_code(verified=true), confirm(email·code·new_password 재검증 후 변경·코드 삭제)
- 엔드포인트: POST /api/v2/ums/auth/password-reset/send·verify·confirm (confirm 요청 필드 newPassword)
- 예외: PasswordResetExpiredException/PasswordResetInvalidException (400)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한

## 회원탈퇴

- 현재(개발 단계): DELETE /api/v2/ums/user/me → 레코드 하드 삭제 (UserService.withdraw). email 즉시 해제되어 재가입 가능 (api-v1과 동일)
- 추후: soft delete(status=DELETED + deleted_date) 전환, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403), 유예 후 PII 익명화/하드삭제 배치, 연관 데이터 비동기 정리, 조회 시 DELETED 필터링, email 재가입 정책

## 커뮤니티 카테고리 (CMS)

- 도메인 app/domain/cms, GET /api/v2/cms/{section}/categories (section: TECH/INVEST/POLITICS) — api-v1 미러링
- categories 테이블 api-v1과 DB 공유, 잘못된 section → 400 INVALID_SECTION

## 커뮤니티 게시글 (PMS)

- 도메인 app/domain/pms, POST /api/v2/pms/{section}/posts (인증 필요, 단일 엔드포인트) — api-v1 미러링
- community_posts / post_categories(N:M), 경계 넘는 참조는 FK 없이 인덱스
- CategoryQueryClient(Protocol)로 cms 카테고리 검증 추상화 (MSA 분리 대비)
- categoryIds 1~3개 필수, section 불일치 → 400 INVALID_CATEGORY

## 빌드/배포

- 빌드: uv sync
- Docker: python:3.12-slim multi-stage
- 배포: GitHub Actions → ECR → ECS
