# yologram-api-v1 구현 계획

## 1단계: 회원가입

### 의존성
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-security-crypto (BCryptPasswordEncoder)
- mysql-connector-j
- java-jwt (Auth0)
- querydsl (kapt)
- testcontainers (MySQL)

### DB 설정
- application.yaml: JPA/Hibernate 공통 (ddl-auto, batch-size, timezone)
- application-local.yaml: 로컬 MySQL (testcontainers 또는 로컬 DB)
- application-prod.yaml: RDS MySQL (Parameter Store 주입)
- R/W splitting: MasterSlaveRoutingDataSource

### 엔티티
- User: id, email, name, nickname, password, avatar, accessToken, type, status, deletedDate, joinedDate, modifiedDate

### API
- POST /api/v1/ums/user/join → 회원가입

### 테스트
- UserService 단위 테스트
- 회원가입 API 통합 테스트 (Testcontainers)

## 2단계: 로그인/로그아웃

### API
- POST /api/v1/ums/auth/login
- POST /api/v1/ums/auth/logout
- POST /api/v1/ums/auth/validate-token (master DB 조회)

### JWT
- HMAC256, Auth0 java-jwt
- 토큰 생성/검증 유틸
- 설정값: application.yaml (secret, expire, issuer, audience)

### 테스트
- AuthService 단위 테스트
- 로그인/로그아웃 API 통합 테스트

## 3단계: 회원정보 조회

### API
- GET /api/v1/ums/user/me (본인 정보 조회, 인증 필요)

### 테스트
- UserService 단위 테스트
- 회원정보 조회 API 통합 테스트

## 4단계: 회원정보 수정

### API
- PATCH /api/v1/ums/user/me (이름, 닉네임 변경, 인증 필요)

### 테스트
- UserService 단위 테스트
- 회원정보 수정 API 통합 테스트

## 5단계: 비밀번호 변경

### API
- PATCH /api/v1/ums/user/me/password (현재 비밀번호 + 새 비밀번호, 인증 필요)

### 테스트
- 비밀번호 변경 서비스 단위 테스트
- 비밀번호 변경 API 통합 테스트

## 6단계: 이메일 인증 (AWS SES)

### 흐름
- 회원가입 전 이메일로 6자리 인증 코드 발송
- 사용자가 인증 코드 입력 → 검증 통과 (verified = true)
- 회원가입 시 해당 이메일의 verified 확인 → 가입 완료 → 인증 레코드 삭제
- email_verification_codes 테이블에 코드 저장 (5분 만료)

### 구조
- EmailSender 인터페이스로 발송 추상화
- StubEmailSender: 로그 출력 (개발/테스트용)
- SesEmailSender: AWS SES 연동 (HTML 이메일 발송)

### API
- POST /api/v1/ums/auth/email-verification/send (이메일 중복 확인 → 코드 생성 → 발송)
- POST /api/v1/ums/auth/email-verification/verify (코드 확인 + 만료 체크 → verified = true)

### 예외
- EmailVerificationExpiredException (400): 코드 만료
- EmailVerificationInvalidException (400): 코드 불일치 또는 레코드 없음
- EmailNotVerifiedException (400): 미인증 상태로 회원가입 시도

### 테스트
- EmailVerificationService 단위 테스트
- AuthResource 슬라이스 테스트 (발송/인증 엔드포인트)
- UserService 이메일 인증 연동 테스트 (미인증 가입 차단)

## 7단계: 비밀번호 찾기 (AWS SES)

### 흐름
- 로그인 페이지에서 이메일 입력 → 비밀번호 재설정 링크/임시 비밀번호 발송

### API
- POST /api/v1/ums/auth/reset-password

## 8단계: Refresh Token

### 흐름
- login 시 access token + refresh token 쌍 발급
- access token 만료 시 refresh token으로 재발급

### API
- POST /api/v1/ums/auth/refresh

## 제외 범위 (이후)

- OAuth (Gmail, Kakao)
- 프로필 이미지 업로드
- 회원 탈퇴
