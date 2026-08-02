# yologram-worker

비동기 워커 서비스. 요청 경로에서 분리할 주기/배치/후속 작업을 담당한다.

- 테크 뉴스 파이프라인(운영 중): RSS 수집(cron 10분) → LLM 요약(cron 5분, 배치 10건) → Discord 채널별 embed 알림
- SQS 컨슈머: API가 큐에 넣은 배치 작업 풀링 — OpenSearch full index, 회원탈퇴 연관 데이터 청크 삭제, 게시글 삭제 시 댓글 정리 이관 (예정)

## 기술 스택

- Kotlin 1.9.25, Spring Boot 3.5.14, Java 17
- Spring Web + Actuator (헬스체크) / WebFlux WebClient (외부 HTTP 호출 통일)
- Spring Data JPA + MySQL (master/slave 라우팅 — api-v1 미러)
- 도메인 경계: 타 도메인(cms) 접근은 infra/client의 CmsApiClient 경유 (api-v1 규칙 미러)
- Rome (RSS/Atom 파싱), Readability4J + jsoup (원문 본문 추출·HTML 파싱)
- Spring AI 1.1.8 (OpenAI 호환 ChatModel — Gemini flash-lite 1순위, Groq llama-3.3 fallback)
- Spring Cloud AWS Parameter Store (설정 주입)
- kotlin-logging (로깅)
- OpenTelemetry (logs/metrics/traces → Grafana Cloud OTLP direct push)
- Gradle (Kotlin DSL), Testcontainers(MySQL)

## 실행

```bash
# 로컬 (포트 5003, Parameter Store 없어도 기동 — OTLP 미전송)
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트
./gradlew test

# 빌드
./gradlew build
```

## 설정

- application.yaml: 공통 (database.main·cron·LLM 모델·Discord 채널 — 비밀값은 Parameter Store에서 주입)
- application-local.yaml: 로컬 (포트 5003, OTLP 비활성, DB는 worker_local SSM)
- application-prod.yaml: 프로덕션 (Parameter Store /yologram/service/yologram-worker_prod/ — OTLP 6·DB 6·LLM 키 2·Discord 웹훅 3)

## 배포

- Docker (amazoncorretto:17-alpine)
- ECS Fargate Spot 0.25vCPU/512MB (yologram-infra aws/services/yologram-worker)
- GitHub Actions: Gradle build → Docker build → ECR push → ECS 재배포
