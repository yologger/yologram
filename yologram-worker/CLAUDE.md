# yologram-worker 프로젝트 지침

## 프로젝트 개요

Spring Boot (Kotlin) 비동기 워커. 주기 작업(@Scheduled) 담당 — SQS 배치 소비는 예정. 번개장터 bun-ums-worker 패턴을 미러링한 구조.
현재 테크 뉴스 파이프라인(RSS 수집 → LLM 요약 → Discord 알림) 운영 중.

> 기술 스택은 README.md, 구현 기능·설계 근거는 루트 docs/done.md, 구현 시 따라야 할 제약·참고는 docs/rules.md 참조.

## 주요 파일

- src/main/kotlin/link/yologram/worker/Application.kt: 엔트리 — JVM 초기화(TimeZone Asia/Seoul, DNS TTL) 후 기동
- config/database/CoreDatabaseConfig.kt: master/slave DataSource 라우팅 (api-v1 미러, database.main.* 프로퍼티)
- config/JpaConfig.kt: @EnableJpaAuditing
- config/OpenTelemetryLoggingConfig.kt: OTEL logback 초기화
- global/client/WebClientFactory.kt·HttpClientConfig.kt: 공용 WebClient (타임아웃·리다이렉트, 수집용 outboundWebClient 빈)
- global/llm/: LlmClient(Gemini→Groq fallback)·LlmConfig(Spring AI OpenAI 호환, read timeout 60초)·LlmProperties(yologram.llm.*)
- global/discord/: DiscordNotifier(채널별 웹훅 send/sendEmbed)·DiscordConfig·DiscordProperties(yologram.discord.webhooks.{채널}.url/enabled)
- domain/tech/news/: 테크 뉴스 도메인 — client(RssFeedClient·NewsContentCrawler), service(Collect·Summarize·CategoryParser), scheduler(cron 수집 10분·요약 5분). LLM 분류 어휘는 tech_category(활성)를 배치마다 로드 — 어드민 카테고리 변경 자동 반영, 매핑은 categoryId
- src/main/resources/application.yaml: 공통 설정 (database.main, cron, LLM 모델, Discord 채널)
- src/main/resources/logback-spring.xml: 로깅 (콘솔 + prod OTEL appender)

## 도메인 구조

- domain/{섹션}/{기능} — tech/news(운영 중), invest·politics(추후 별도 테이블·수집기), pms/search/ums(기능 도메인, 예정)
- 클래스명은 섹션 접두사 유지(TechNews...) — 추후 InvestNews/PoliticsNews와 JPA 엔티티 단순명 충돌 방지
- 테이블: tech_news_source + tech_news + tech_news_category_mapping(categoryId) + tech_category(공용 마스터, 조회 전용). 전 테이블 FK 미사용(같은 도메인 포함), status(COLLECTED/SUMMARIZED/FAILED)+retry_count가 작업 큐

## 작업 규칙

- 워커 작업은 멱등·재시도 가능하게 설계 (FARGATE_SPOT 중단 전제 — 2분 경고 후 종료·재기동)
- @Scheduled는 놓친 회차를 소급하지 않음 — 다음 회차가 커버하는 작업(RSS 등)만. 시각 민감/누적형 배치는 EventBridge Scheduler → SQS로
- 주기 작업은 단일 인스턴스 전제 — 인스턴스 확장 시 ShedLock 도입 (todos)
- HTTP 호출은 WebClient 통일(WebClientFactory — 예외: Spring AI는 자체 RestClient에 타임아웃 주입). LLM 모델 선정은 실측 기준(/v1/models 조회, 429 quotaValue) — docs/rules.md 참조
- SQS 컨슈머는 EventHandler(canHandle/handle) 라우팅 패턴으로 (해당 기능 진행 시)

## 설정 관리

- application-local.yaml: 로컬 (포트 5003, OTLP 비활성, worker_local SSM만 import — prod 파라미터 유입 금지)
- application-prod.yaml: 프로덕션 (Parameter Store /yologram/service/yologram-worker_prod/ — OTLP 6·DB 6·LLM 키 2·Discord 웹훅 3)
- Discord 채널 on/off는 yaml(webhooks.{채널}.enabled), URL은 SSM

## 테스트

- 신규 기능 구현 시 모든 케이스(정상/예외/엣지)에 대해 테스트코드 작성
- Testcontainers(MySQL) + 단위(mockito-kotlin, WebClient는 exchangeFunction 목)
- 테스트 프로파일(test): OTLP 비활성, 스케줄러 cron "-"(CRON_DISABLED)

## 로컬 개발

- 포트: 5003 (api-v1: 5001, api-v2: 5002와 구분)
- ./gradlew bootRun --args='--spring.profiles.active=local'

## 배포

- Docker (amazoncorretto:17-alpine, jar copy only)
- ECS Fargate Spot 0.25vCPU/512MB (yologram-infra aws/services/yologram-worker — 인바운드 없음, API GW/Cloud Map 미사용)
- GitHub Actions: Gradle build → Docker build → ECR push → ECS 재배포
