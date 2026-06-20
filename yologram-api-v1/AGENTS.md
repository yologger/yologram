# yologram-api-v1 에이전트 가이드

## 프로젝트 개요

Spring Boot MVC (Kotlin) API 서버. ECS Fargate에서 운영.

## 주요 파일

- src/main/resources/application.yaml: 공통 설정 (OTLP endpoint, resource attributes)
- src/main/resources/logback-spring.xml: 로깅 설정 (콘솔 + OTEL appender)
- src/main/kotlin/.../config/OpenTelemetryLoggingConfig.kt: OTEL logback 초기화
- src/main/kotlin/.../domain/ums/service/AuthService.kt: JWT 로그인/로그아웃/토큰 검증. validate-token은 master DB 조회

## 코드 컨벤션

- Kotlin 코드 스타일
- 로깅: kotlin-logging-jvm (io.github.oshai)
- 설정값: AWS Parameter Store에서 Spring property로 주입
- 조금이라도 복잡한 쿼리는 QueryDSL 우선 (스터디 목적)

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

- 도메인 domain/cms, GET /api/v1/cms/{section}/categories (section: TECH/INVEST/POLITICS)
- Section enum은 domain/cms/enums (패키지명 `enum`은 Java 예약어라 QueryDSL Q클래스에서 enum 필드 누락 → enums 사용)
- categories 테이블, 잘못된 section → 400 INVALID_SECTION

## 커뮤니티 게시글 (PMS)

- 도메인 domain/pms, POST /api/v1/pms/{section}/posts (인증 필요, 단일 엔드포인트)
- community_posts / post_categories(N:M), 경계 넘는 참조는 FK 없이 인덱스
- CategoryQueryClient로 cms 카테고리 검증 추상화 (MSA 분리 대비)
- 카테고리 section 불일치 → 400 INVALID_CATEGORY
- @AuthenticatedUser 인증 예외는 GlobalExceptionHandler에서 전역 처리
- 상세 조회 GET /api/v1/pms/{section}/posts/{id} (공개), 작성자 닉네임은 UserQueryClient로 ums 조회, 없으면 404 POST_NOT_FOUND
- 목록 조회 GET /api/v1/pms/{section}/posts (공개), 최신순(id desc) + cursor(keyset) 페이지네이션 + categoryId 필터, size 기본 20·최대 50
- 커서/종료는 legacy 방식: id-only 커서 + 마지막 글 id를 nextCursor로(빈 결과면 null), 잘못된 커서 → 400 INVALID_CURSOR
- PostRepositoryImpl이 첫 QueryDSL 사용처(QuerydslConfig의 JPAQueryFactory), N+1 회피 배치 조회, 인덱스 (section, id)

## 빌드/배포

- 빌드: ./gradlew build
- Docker: amazoncorretto:17 multi-stage
- 배포: GitHub Actions → ECR → ECS
