## 인프라

- [ ] GitHub Actions 빌드 캐시 적용: Docker 레이어 캐시 (docker/build-push-action)

## Observability

- [x] Grafana Cloud 연동 - Logs: OTLP (opentelemetry-sdk + OTLPLogExporter)
- [x] Grafana Cloud 연동 - Traces: OTLP (opentelemetry-sdk + OTLPSpanExporter)
- [x] Grafana Cloud 연동 - Metrics: OTLP (opentelemetry-sdk + OTLPMetricExporter)

## DB 설정

- [x] 의존성 추가 (sqlalchemy, pymysql)
- [x] Settings에 DB 필드 추가
- [x] SQLAlchemy engine + SessionLocal
- [x] get_db 의존성 함수

## 공통

- [x] ApiEnvelop 응답 래퍼
- [x] 예외 처리 (UserDuplicateException → 409)
- [x] CORS 전체 허용

## UMS - 회원가입

- [x] User 모델 (SQLAlchemy)
- [x] UserType, UserStatus enum
- [x] JoinRequest, JoinResponse schema
- [x] UserRepository
- [x] UserService.join()
- [x] POST /api/v2/ums/user/join router
- [x] 회원가입 테스트 (service, router)

## UMS - 로그인/로그아웃/토큰검증 (2단계)

- [x] Settings에 jwt_secret, jwt_expire, jwt_issuer, jwt_audience 추가
- [x] PyJWT 의존성 추가
- [x] jwt_util.py (create_token, validate_and_get_uid)
- [x] 인증 스키마 (LoginRequest, LoginResponse, ValidateTokenResponse, AuthData)
- [x] 인증 예외 (AuthWrongPasswordException, AuthTokenExpiredException, AuthTokenInvalidException)
- [x] 인증 의존성 (get_authenticated_user - Bearer 토큰 추출/검증)
- [x] AuthService (login, validate_token, logout)
- [x] POST /api/v2/ums/auth/login
- [x] POST /api/v2/ums/auth/validate-token
- [x] POST /api/v2/ums/auth/logout (204)
- [x] jwt_util 단위 테스트 (4개)
- [x] auth_service 단위 테스트 (8개)
- [x] auth_router E2E 테스트 (10개)

## UMS - 회원정보 조회 (3단계)

- [x] GET /api/v2/ums/user/me (본인 정보 조회)
- [x] 회원정보 조회 테스트
- [x] Swagger 문서화

## UMS - 회원정보 수정 (4단계)

- [x] PATCH /api/v2/ums/user/me (닉네임 변경)
- [x] 회원정보 수정 테스트
- [x] Swagger 문서화

## UMS - 비밀번호 변경 (5단계)

- [x] PATCH /api/v2/ums/user/me/password (현재 비밀번호 + 새 비밀번호)
- [x] 비밀번호 변경 테스트
- [x] Swagger 문서화

## UMS - 이메일 인증 (6단계)

- [x] AWS SES 연동 (이메일 발송 서비스, boto3)
- [x] 인증 코드 생성/저장 로직 (user_email_verification 테이블, 5분 만료)
- [x] POST /api/v2/ums/auth/email-verification/send
- [x] POST /api/v2/ums/auth/email-verification/verify
- [x] 회원가입 시 이메일 인증 필수화
- [x] 이메일 인증 테스트
- [x] Swagger 문서화

## UMS - 회원탈퇴

- [x] DELETE /api/v2/ums/user/me (본인 탈퇴, 개발 단계: 레코드 하드 삭제 → email 즉시 해제·재가입 가능)
- [x] 회원탈퇴 테스트
- [x] Swagger 문서화
- [ ] (추후) soft delete 방식 전환: status=DELETED + deleted_date, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403)
- [ ] (추후) 유예기간 후 PII 익명화/하드삭제 배치, email 재가입 정책
- [ ] (추후) 연관 데이터 정리 비동기 처리 (게시글 등, 이벤트/큐 기반) — 게시글 도메인 추가 후
- [ ] (추후) 조회 시 DELETED 유저 데이터 필터링

## UMS - 비밀번호 찾기 (7단계)

- [x] UserPasswordResetCode 모델 (user_password_reset_code 테이블)
- [x] UserPasswordResetCodeRepository
- [x] EmailSender.send_password_reset_code (Stub/Ses 구현)
- [x] UserPasswordResetService (send_code, verify_code, confirm)
- [x] POST /api/v2/ums/auth/password-reset/send (미가입 시 404)
- [x] POST /api/v2/ums/auth/password-reset/verify
- [x] POST /api/v2/ums/auth/password-reset/confirm (email, code, newPassword 재검증)
- [x] 예외 (PasswordResetExpired/Invalid)
- [x] 비밀번호 찾기 테스트 (service, router)
- [x] Swagger 문서화
- [ ] (운영 보강) 코드 해시 저장, 레이트리밋, 시도 횟수 제한
- [ ] (운영 보강) 코드 발송 재발송 최소 간격 제한 (최근 발송 후 N초 이내 429, email-verification·password-reset 공통)

## UMS - Refresh Token (8단계)

- [ ] refresh token 발급 로직 (login 시 access + refresh 쌍 발급)
- [ ] POST /api/v2/ums/auth/refresh (refresh token으로 access token 재발급)
- [ ] refresh token 저장/검증 로직
- [ ] 로그아웃 시 refresh token 폐기 (서버측 토큰 무효화) — 현재 access token은 stateless라 무효화 불가, refresh 도입과 함께 구현
- [ ] refresh token 테스트
- [ ] Swagger 문서화

## CMS - 커뮤니티 카테고리 (api-v1 미러링)

- [x] Section enum (TECH / INVEST / POLITICS)
- [x] PostCategory 모델 (post_category 테이블, api-v1과 공유)
- [x] CategoryRepository (section별 활성 카테고리 sort_order 정렬 조회)
- [x] PostCategoryService (section_path → Section.from_path 검증)
- [x] GET /api/v2/cms/{section}/categories
- [x] InvalidSectionException (400 INVALID_SECTION)
- [x] 테스트 (service 4 + router 3)
- [x] Swagger 문서화

## PMS - 커뮤니티 게시글 작성 (api-v1 미러링)

- [ ] post / post_category_mapping 테이블 (api-v1과 공유, DB 직접 실행)
- [x] Post / PostCategoryMapping 모델 (FK 없는 인덱스 매핑)
- [x] PostRepository / PostCategoryMappingRepository
- [x] PostCategoryQueryClient(Protocol) + LocalPostCategoryQueryClient (cms 경계 추상화, MSA 대비)
- [x] PostService.create (작성자=인증유저, categoryIds section 일치 검증, 1~3개 필수)
- [x] POST /api/v2/pms/{section}/posts (인증 필요)
- [x] InvalidPostCategoryException (400 INVALID_POST_CATEGORY)
- [x] 검증 실패 응답 400 VALIDATION_ERROR로 통일 (api-v1 정합, 메시지 단일 문자열화)
- [x] 테스트 (service 3 + router 7)

## PMS - 게시글 상세 조회 (api-v1 미러링)

- [x] GET /api/v2/pms/{section}/posts/{id} (공개)
- [x] PostDetailResponse (author{uid,nickname} 포함, categoryIds는 프론트 매핑)
- [x] UserQueryClient(Protocol) + LocalUserQueryClient (작성자 닉네임, ums 경계 추상화 MSA 대비)
- [x] PostRepository.find_by_id, PostCategoryRepository.find_by_post_id 추가
- [x] PostNotFoundException (404, POST_NOT_FOUND), id가 해당 section 글 아니면 404
- [x] 테스트 (service 3 + router 2)
- [x] GET /api/v2/pms/{section}/posts (목록, cursor 페이지네이션) — api-v1 미러링, 최신순(id desc) + id-only 커서 + categoryId 필터(EXISTS), size 1~50
- [x] ApiEnvelopCursorPage[T]{data, nextCursor} / PostSummaryResponse(content 포함) / PostCursor(id-only Base64)
- [x] 커서/종료 legacy 방식: 마지막 글 id를 nextCursor로(빈 결과면 null), 잘못된 커서 400 INVALID_CURSOR
- [x] N+1 회피: find_nicknames(UserRepository.find_by_ids) · find_by_post_ids 배치 조회
- [x] 테스트 (service 5 + router 2)

## Admin - 유저 관리

- [ ] GET /api/v2/ums/admin/users (유저 목록 조회)
- [ ] GET /api/v2/ums/admin/users/{uid} (유저 상세 조회)
- [ ] PATCH /api/v2/ums/admin/users/{uid} (유저 정보 수정)
- [ ] DELETE /api/v2/ums/admin/users/{uid} (유저 삭제)
- [ ] 어드민 권한 검증 (UserType.ADMIN)
- [ ] 테스트
- [ ] Swagger 문서화

## Admin - 게시글 관리

- [ ] GET /api/v2/admin/posts (게시글 목록 조회)
- [ ] GET /api/v2/admin/posts/{id} (게시글 상세 조회)
- [ ] DELETE /api/v2/admin/posts/{id} (게시글 삭제)
- [ ] 테스트
- [ ] Swagger 문서화
