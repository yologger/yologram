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

## 비밀번호 찾기

- 방식: 이메일 6자리 코드 발송 → 코드 검증 → 새 비밀번호 설정 (회원가입 이메일 인증과 동일 패턴/SES 재사용)
- 저장: 별도 테이블 password_reset_codes (email, code 6자리, verified, expiredAt 5분, createdAt) — 회원가입 인증 로직과 분리해 회귀 위험 최소화
- 흐름:
  - send: 미가입 이메일이면 404 USER_NOT_FOUND, 기존 코드 삭제 후 새 코드 발송
  - verify: 코드 검증 → verified=true (프론트 단계 게이팅용)
  - confirm: (email, code, newPassword) 최종 단계에서 코드·만료 재검증 후 비밀번호 변경, 코드 삭제
- 보강 TODO(운영): 코드 평문 대신 해시 저장, 발송 레이트리밋, 시도 횟수 제한/잠금 (현재는 email_verification_codes와 동일하게 평문·무제한)

## 회원탈퇴 데이터 정리 전략 (구현 시 결정)

탈퇴 요청 처리와 연관 데이터(게시글/댓글/좋아요 등) 삭제를 분리해 요청 부하를 낮춘다.

- 1차(동기, 즉시 응답): User.status=DELETED, deletedDate 기록, accessToken 무효화 후 204 반환. 조회 단계에서 DELETED 유저 데이터는 필터링 → 사용자 입장에선 즉시 탈퇴.
- 2차(비동기, 연관 데이터 실제 삭제) 옵션:
  - SQS 이벤트 + 워커 (권장, 확장성): "user-deleted" 발행 → 워커가 도메인별 배치 삭제, 실패 시 DLQ 재시도
  - 배치 잡 (간단): DELETED 유저를 주기적으로 스캔해 청크 삭제, 별도 인프라 최소
  - 앱 내 @Async (소규모/임시): 요청 스레드와 분리하나 인스턴스 종속 → 배포/장애 시 유실 위험, 대량 부적합
- 대량 삭제는 청크(LIMIT N) 단위 반복으로 락/replica 지연 완화
- 보관 의무 데이터는 삭제 대신 익명화, 유예기간(복구) 정책 검토 (soft delete면 자연스럽게 지원)
- 권장 조합: soft delete 즉시 응답 + SQS 워커의 도메인별·청크 삭제 (규모 작으면 배치 잡으로 시작)
