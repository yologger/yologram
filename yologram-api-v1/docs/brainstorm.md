# yologram-api-v1 브레인스토밍

## 기술 스택

- Spring Boot 3.5 (Kotlin), Java 17
- Spring Data JPA (Hibernate) - 기본 CRUD
- QueryDSL - 복잡한 쿼리
- MySQL (RDS) - 프로덕션 DB
- Testcontainers (MySQL) - 로컬/CI 테스트
- BCryptPasswordEncoder - 비밀번호 해싱
- Auth0 java-jwt - JWT 토큰

## DB

- R/W splitting: MasterSlaveRoutingDataSource (레거시 패턴)
- @Transactional(readOnly = true) → Slave, 그 외 → Master
- 로컬: Testcontainers MySQL
- 프로덕션: RDS MySQL (Parameter Store에서 credential 주입)

## 도메인 구조

- UMS (User Management Service): 회원가입, 로그인, 로그아웃, 토큰 관리
- 추후: BMS (Board), NMS (News) 등 마이크로서비스 경로 분리
- 실제로는 api-v1 단일 서버에서 모두 처리 (토이프로젝트)

## API 경로 규칙

- /api/v1/ums/ - 유저 관리
- /api/v1/bms/ - 게시판 관리 (추후)
- /api/v1/nms/ - 뉴스 관리 (추후)

## 유저 타입

- DEFAULT: 일반 사용자
- POLITICIAN: 정치인
- ECONOMIST: 경제인
- ADMIN: 관리자

## 인증 방식

- JWT (HMAC256)
- 토큰은 User 엔티티에 캐시 (accessToken 필드)
- 커스텀 헤더: Authorization Bearer 방식
- 로그아웃 시 accessToken null 처리

## 이메일 인증

- EmailSender 인터페이스로 발송 추상화 (테스트/개발: StubEmailSender, 프로덕션: SesEmailSender)
- email_verification_codes 테이블: email, code(6자리 숫자), verified, expiredAt
- 인증 흐름: 코드 발송 → 코드 검증 → verified=true → 회원가입 시 확인
- 같은 이메일로 재발송 시 기존 레코드 삭제 후 새로 생성
- 가입 완료 후 인증 레코드 삭제
