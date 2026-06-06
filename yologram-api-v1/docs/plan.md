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

## 3단계: 유저 조회/탈퇴

### API
- GET /api/v1/ums/user/{uid}
- DELETE /api/v1/ums/user/withdraw

## 제외 범위 (이후)

- OAuth (Gmail, Kakao)
- 비밀번호 변경
- 프로필 이미지 업로드
