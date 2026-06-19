# yologram-api-v1 프로젝트 지침

## 기술 스택

- Spring Boot 3.5 (Kotlin), Java 17
- Gradle (Kotlin DSL)
- Spring Data JPA + QueryDSL (ORM + 복잡 쿼리). 조금이라도 복잡한 쿼리는 QueryDSL 우선 (스터디 목적)
- MySQL (RDS) + Testcontainers (테스트)
- R/W splitting (MasterSlaveRoutingDataSource)
- BCryptPasswordEncoder (비밀번호 해싱)
- Auth0 java-jwt (JWT 토큰)
- kotlin-logging (로깅)
- springdoc-openapi (Swagger)
- AWS SDK v2 (SES 이메일 발송)

## 설정 관리

- application.yaml: 공통 설정 (OTLP endpoint placeholder)
- application-local.yaml: 로컬 개발 (AWS Parameter Store)
- application-prod.yaml: 프로덕션 (AWS Parameter Store, instance-profile)
- 설정값은 AWS Parameter Store에서 주입 (/yologram/service/yologram-api-v1_{ENV}/)

## Observability

- Grafana Cloud OTLP direct push
- Logs: opentelemetry-logback-appender + OpenTelemetryLoggingConfig
- Metrics: micrometer-registry-otlp
- Traces: micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp
- Resource 속성: service.name, deployment.environment.name, service.instance.id, service.namespace

## 인증

- JWT: Auth0 java-jwt (HMAC256)
- 설정: yologram.auth.jwt.secret/expire/issuer/audience (Parameter Store + application.yaml)
- 인증 헤더: Authorization: Bearer {token}
- @AuthenticatedUser + AuthenticatedUserResolver로 인증 정보 주입
- @AuthenticatedUser 인증 예외(AuthToken*)는 GlobalExceptionHandler에서 전역 처리 (ums 외 도메인 컨트롤러에서도 401 보장)
- access token은 stateless JWT (서버에 저장하지 않음). 로그아웃은 클라이언트가 토큰을 폐기하는 방식이며, 현재 서버측 강제 무효화는 불가
- validate-token: JWT 서명/만료 검증 + 사용자 존재 확인
- (추후) refresh token 도입 시 서버측 토큰 무효화도 함께 구현 (로그아웃 시 refresh token 폐기)
- validate-token은 로그인 직후 replica lag를 피하기 위해 master DB 트랜잭션으로 조회

## 이메일 인증

- EmailSender 인터페이스로 발송 추상화
- @Profile("!prod") StubEmailSender: 로그 출력 (개발/테스트용)
- @Profile("prod") SesEmailSender: AWS SES 발송 (SesConfig에서 SesClient 빈 수동 등록)
- 발신 주소: no-reply@yologram.link (IAM 정책으로 한정, 변경 시 인프라 수정 필요)
- 리전: ap-northeast-2 (SES 도메인 인증 리전과 동일)
- 자격증명: ECS Task Role (prod), AWS_PROFILE 환경변수 (로컬)
- EmailVerificationCode 엔티티: email, code(6자리), verified, expiredAt(5분)
- 회원가입 시 이메일 인증 필수 (UserService.join에서 verified 확인)

## 비밀번호 찾기

- 방식: 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (이메일 인증과 동일 패턴/SES 재사용)
- 저장: 별도 테이블 password_reset_codes (PasswordResetCode 엔티티: email, code, verified, expiredAt 5분, createdAt)
- PasswordResetService: sendCode(미가입 시 UserNotFoundException 404, 기존 코드 삭제 후 발송), verifyCode(verified=true), confirm(email·code·newPassword 재검증 후 변경·코드 삭제)
- 엔드포인트: POST /api/v1/ums/auth/password-reset/send·verify·confirm
- 예외: PasswordResetExpiredException/PasswordResetInvalidException (400)
- 운영 보강 TODO: 코드 해시 저장, 발송 레이트리밋, 시도 횟수 제한

## 회원탈퇴

- 현재(개발 단계): DELETE /api/v1/ums/user/me → 레코드 하드 삭제 (UserService.withdraw). email 즉시 해제되어 재가입 가능
- 추후: soft delete(status=DELETED + deletedDate) 전환, 탈퇴 유저 login/validate 차단(USER_WITHDRAWN 403), 유예 후 PII 익명화/하드삭제 배치, 연관 데이터 비동기 정리, 조회 시 DELETED 필터링, email 재가입 정책

## 커뮤니티 카테고리 (CMS)

- 도메인: domain/cms (Section enum, Category 엔티티/조회)
- Section enum: TECH / INVEST / POLITICS (게시판=섹션, @Enumerated(STRING) VARCHAR(20)). 코드 결합이라 ENUM 유지
- categories 테이블: id, section, name, sort_order, is_active, created_at, UNIQUE(section, name)
- CategoryService.getCategories(sectionPath): Section.fromPath로 검증(대소문자 무시), isActive=true, sortOrder 정렬
- 응답 CategoryResponse: { id, name, sortOrder }
- 엔드포인트: GET /api/v1/cms/{section}/categories
- 예외: InvalidSectionException (400, INVALID_SECTION) — CmsExceptionHandler
- 카테고리는 어드민이 관리하는 콘텐츠(추후 CRUD), 프론트는 이 API로 섹션별 필터를 동적 렌더
- 도메인 분리 전략(pms/cms/comment/count/news)·하이브리드 스키마 결정은 docs/brainstorm.md 참조

## 커뮤니티 게시글 (PMS)

- 도메인: domain/pms (Post, PostCategory). 작성은 단일 엔드포인트 POST /api/v1/pms/{section}/posts (인증 필요)
- community_posts (단일 + section): id, section, user_id, title, content, like_count, comment_count, created_at, modified_date / 인덱스 (section, created_at)
- post_categories (N:M): post_id, category_id — 카테고리 필터 조회용. FK 제약 없이 인덱스만(경계 분리 대비)
- 도메인 경계(ums user_id, cms category_id)를 넘는 참조는 FK 없이 컬럼+인덱스
- PostService.create: 작성자=인증 유저(uid), categoryIds가 해당 section 활성 카테고리인지 검증(1~3개 필수, 미선택 시 프론트가 '기타' 자동 지정 예정)
- CategoryQueryClient 인터페이스로 cms 카테고리 검증 추상화 → 모놀리식은 LocalCategoryQueryClient(cms 리포지토리 직접), MSA 분리 시 HTTP 호출 구현으로 교체
- 요청 { title?, content, categoryIds[] }, 응답 { id } (201)
- 예외: InvalidCategoryException (400, INVALID_CATEGORY), 잘못된 section은 Section.fromPath의 InvalidSectionException (400) — PmsExceptionHandler
- section별 전용 필드(투자 종목코드 등)는 추후 확장 테이블 + 동일 엔드포인트 body 확장으로 처리(엔드포인트 분리 X)

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- Testcontainers (MySQL) 통합 테스트
- Mockito + MockMvc 슬라이스 테스트

## Swagger

- Swagger UI: /api/v1/docs
- api-docs: /api/v1/api-docs

## CORS

- WebConfig에서 전체 허용 (*) — 인증 구현 시 origin 제한 필요

## 포트

- 기본(ECS): 5000
- 로컬: 5001 (application-local.yaml에서 override)

## 배포

- Docker (amazoncorretto:17-alpine, jar copy only)
- ECS Fargate
- GitHub Actions: Gradle build (--build-cache) → ECR push → ECS 재배포
