# yologram-api-v1 프로젝트 지침

## 프로젝트 개요

Spring Boot MVC (Kotlin) API 서버. ECS Fargate에서 운영.

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- src/main/resources/application.yaml: 공통 설정 (OTLP endpoint, resource attributes)
- src/main/resources/logback-spring.xml: 로깅 설정 (콘솔 + OTEL appender)
- src/main/kotlin/.../config/OpenTelemetryLoggingConfig.kt: OTEL logback 초기화
- src/main/kotlin/.../config/QuerydslConfig.kt: JPAQueryFactory 빈
- src/main/kotlin/.../domain/ums/service/AuthService.kt: JWT 로그인/로그아웃/토큰 검증. validate-token은 master DB 조회
- src/main/kotlin/.../domain/ums/service/UserService.kt: 회원가입(이메일 인증 확인)·정보 수정·비밀번호 변경·회원탈퇴
- src/main/kotlin/.../domain/ums/service/UserEmailVerificationService.kt + EmailSender(Stub/Ses)·SesConfig: 이메일 인증·발송
- src/main/kotlin/.../domain/ums/service/UserPasswordResetService.kt: 비밀번호 찾기
- src/main/kotlin/.../domain/cms: Section enum(cms/enums 패키지), PostCategoryService
- src/main/kotlin/.../domain/pms: PostService(작성/상세/목록), PostRepositoryImpl(QueryDSL)

## 설정 관리

- application.yaml: 공통 설정 (OTLP endpoint placeholder)
- application-local.yaml: 로컬 개발 (AWS Parameter Store)
- application-prod.yaml: 프로덕션 (AWS Parameter Store, instance-profile)
- 설정값은 AWS Parameter Store에서 주입 (/yologram/service/yologram-api-v1_{ENV}/)

## 인증 (코딩 규칙)

- JWT: Auth0 java-jwt (HMAC256), 인증 헤더 Authorization: Bearer {token}
- 설정: yologram.auth.jwt.secret/expire/issuer/audience (Parameter Store + application.yaml)
- @AuthenticatedUser + AuthenticatedUserResolver로 인증 정보 주입
- 인증 예외(AuthToken*)는 GlobalExceptionHandler에서 전역 처리 (ums 외 도메인 컨트롤러에서도 401 보장)
- access token은 stateless JWT (서버 미저장). validate-token은 로그인 직후 replica lag 회피 위해 master DB 트랜잭션으로 조회
- (동작·정책·refresh token 계획은 docs/done.md, docs/todos.md 참조)

## 이메일 인증 / SES (코딩 규칙)

- EmailSender 인터페이스로 발송 추상화: @Profile("!prod") StubEmailSender(로그), @Profile("prod") SesEmailSender(SesConfig에서 SesClient 빈 수동 등록)
- 발신 주소: no-reply@yologram.link (IAM 정책으로 한정, 변경 시 인프라 수정 필요)
- 리전: ap-northeast-2 (SES 도메인 인증 리전과 동일)
- 자격증명: ECS Task Role (prod), AWS_PROFILE 환경변수 (로컬)
- 비밀번호 찾기도 동일 패턴/SES 재사용 (UserPasswordResetService)

## 커뮤니티 (cms/pms 코딩 규칙·함정)

- Section enum은 domain/cms/enums 패키지에 둔다 — 패키지명 `enum`(Java 예약어) 금지. QueryDSL APT가 import를 생성 못 해 Q클래스에서 enum 필드가 누락됨 (cms.enum → cms.enums)
- 도메인 경계(ums user_id, cms category_id)를 넘는 참조는 FK 없이 컬럼+인덱스. 경계 검증·조회는 QueryClient로 추상화 (LocalPostCategoryQueryClient, UserQueryClient)
- PostRepositoryImpl이 첫 QueryDSL 사용처. N+1 회피 위해 닉네임(findNicknames)·카테고리(findByPostIds) 배치 조회
- 인덱스: post (section, id) = idx_post_section_id (id desc 정렬·커서 커버)
- (데이터 모델·엔드포인트·설계 근거는 docs/done.md, QueryDSL 사용 기준·경로 규칙은 docs/rules.md 참조)

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- Testcontainers (MySQL) 통합 테스트
- Mockito + MockMvc 슬라이스 테스트

## Swagger

- Swagger UI: /api/v1/docs
- api-docs: /api/v1/api-docs
- 신규 API 추가 시 Swagger 문서화 필수

## CORS

- WebConfig에서 전체 허용 (*) — 인증 구현 시 origin 제한 필요

## 배포

- Docker (amazoncorretto:17-alpine, jar copy only)
- ECS Fargate
- GitHub Actions: Gradle build (--build-cache) → ECR push → ECS 재배포
