# yologram-api-v1

Spring Boot MVC (Kotlin) API 서버.

## 사전 준비

- Java 17+
- Gradle

## 로컬 실행

```bash
./gradlew bootRun
```

서버 기본 주소: http://localhost:5001

## 테스트

```bash
./gradlew test
```

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | /api/v1/ums/user/join | 회원가입 |
| POST | /api/v1/ums/auth/login | 로그인 |
| POST | /api/v1/ums/auth/validate-token | 토큰 검증 |
| POST | /api/v1/ums/auth/logout | 로그아웃 |
| POST | /api/v1/ums/auth/email-verification/send | 이메일 인증 코드 발송 |
| POST | /api/v1/ums/auth/email-verification/verify | 이메일 인증 코드 검증 |
| POST | /api/v1/ums/auth/password-reset/send | 비밀번호 재설정 코드 발송 |
| POST | /api/v1/ums/auth/password-reset/verify | 비밀번호 재설정 코드 검증 |
| POST | /api/v1/ums/auth/password-reset/confirm | 비밀번호 재설정 |
| GET | /api/v1/ums/user/me | 회원정보 조회 |
| PATCH | /api/v1/ums/user/me | 회원정보 수정 |
| PATCH | /api/v1/ums/user/me/password | 비밀번호 변경 |
| DELETE | /api/v1/ums/user/me | 회원탈퇴 (개발 단계: 하드 삭제) |

API 문서: http://localhost:5001/api/v1/docs

## 이메일 인증

- 회원가입 전 이메일 인증 필수 (6자리 코드, 5분 유효)
- EmailSender 인터페이스로 발송 추상화
- @Profile("prod") SesEmailSender: AWS SES로 HTML 이메일 발송
- @Profile("!prod") StubEmailSender: 로그 출력 (개발/테스트용)

## 비밀번호 찾기

- 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (5분 유효)
- 미가입 이메일로 발송 시 404, confirm 단계에서 코드·만료 재검증
- 저장: password_reset_codes 테이블 (재발송 시 기존 코드 삭제 후 새로 생성, 변경 완료 시 삭제)

## Auth

- JWT 인증은 `Authorization: Bearer {token}` 헤더 사용
- validate-token은 로그인 직후 replica lag를 피하기 위해 master DB 트랜잭션으로 조회

## Observability (Grafana Cloud, OTLP)

| 구분 | 라이브러리 |
|---|---|
| 자동 계측 | spring-boot-starter-actuator |
| Logs | opentelemetry-logback-appender |
| Traces | micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp |
| Metrics | micrometer-registry-otlp |

설정값은 AWS Parameter Store에서 주입 (`/yologram/service/yologram-api-v1_{profile}/`).

### 프로필별 동작

- local: 콘솔 로그만 출력
- prod: 콘솔 + Grafana Cloud (OTLP) 전송


