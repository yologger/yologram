# yologram-worker 프로젝트 지침

## 프로젝트 개요

Spring Boot (Kotlin) 비동기 워커. 주기 작업(@Scheduled)과 SQS 배치 소비를 담당. 번개장터 bun-ums-worker 패턴을 미러링한 구조.

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- src/main/kotlin/link/yologram/worker/Application.kt: 엔트리 — JVM 초기화(TimeZone Asia/Seoul, DNS TTL) 후 기동
- src/main/kotlin/link/yologram/worker/config/OpenTelemetryLoggingConfig.kt: OTEL logback 초기화
- src/main/resources/application.yaml: 공통 설정 (OTLP placeholder)
- src/main/resources/logback-spring.xml: 로깅 (콘솔 + prod OTEL appender)

## 작업 규칙

- 워커 작업은 멱등·재시도 가능하게 설계 (FARGATE_SPOT 중단 전제 — 2분 경고 후 종료·재기동)
- @Scheduled는 놓친 사이클을 소급하지 않음 — 다음 주기가 커버하는 작업(RSS 등)만. 시각 민감/누적형 배치는 EventBridge Scheduler → SQS로
- 주기 작업은 단일 인스턴스 전제 — 인스턴스 확장 시 ShedLock 검토
- SQS 컨슈머는 EventHandler(canHandle/handle) 라우팅 패턴으로 (해당 기능 진행 시)
- DB 접근이 필요해지면(news 등) JPA 추가 — 도메인 경계는 FK 없이 컬럼+인덱스 규칙 유지

## 설정 관리

- application-local.yaml: 로컬 (포트 5003, OTLP 비활성, Parameter Store optional)
- application-prod.yaml: 프로덕션 (Parameter Store /yologram/service/yologram-worker_prod/)
- 설정값은 AWS Parameter Store에서 주입

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- 테스트 프로파일(test)은 OTLP·tracing 비활성 (src/test/resources/application-test.yaml)

## 로컬 개발

- 포트: 5003 (api-v1: 5001, api-v2: 5002와 구분)
- ./gradlew bootRun --args='--spring.profiles.active=local'

## 배포

- Docker (amazoncorretto:17-alpine, jar copy only)
- ECS Fargate Spot 0.25vCPU/512MB (yologram-infra aws/services/yologram-worker — 인바운드 없음, API GW/Cloud Map 미사용)
- GitHub Actions: Gradle build → Docker build → ECR push → ECS 재배포
