plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "link.yologram.worker"
version = "0.0.1-SNAPSHOT"
description = "yologram-worker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web (actuator 헬스체크용 HTTP)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // WebClient (Discord 웹훅 등 외부 호출 — 서버는 MVC 유지)
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JPA + MySQL
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")

    // Redis(Valkey) — 캐시 무효화 발행 (Lettuce, 수동 빈 구성은 RedisConfig — api-v1 미러)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // RSS 파싱
    implementation("com.rometools:rome:2.1.0")

    // LLM 요약 (Spring AI OpenAI 호환 ChatModel — Gemini/Groq, Boot 3.5 호환 1.1.x)
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.8"))
    implementation("org.springframework.ai:spring-ai-openai")

    // 원문 본문 추출 (요약 입력 — 로드는 WebClient, 추출은 Readability. jsoup은 명시 버전 고정)
    implementation("org.jsoup:jsoup:1.22.2")
    implementation("net.dankito.readability4j:readability4j:1.0.8")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")

    // Observability
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.15.0-alpha")
    implementation("io.micrometer:micrometer-registry-otlp")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Spring Cloud AWS
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.0"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-parameter-store")
    // SQS 소비 — @SqsListener(수동 ack·가시성 조정). 레거시 BoardIndexingHandler와 같은 스택
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    // OpenSearch 공식 Java 클라이언트. 레거시가 쓰던 RestHighLevelClient는 지원 종료 계열이라 채택하지 않는다
    implementation("org.opensearch.client:opensearch-java:2.25.0")
    // opensearch-java의 transport 구현 — 클라이언트가 optional 의존으로 두어 직접 선언해야 한다
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.1")

    // Kinesis
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.0"))
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kinesis:4.0.4")
    implementation("org.springframework.integration:spring-integration-aws:3.0.9")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:junit-jupiter")
    // Kinesis·DynamoDB 소비 경로 통합 검증 (조회 이벤트 바인더 — PostViewEventConsumerIntegrationTest)
    testImplementation("org.testcontainers:localstack")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // 런타임(Application.kt에서 Asia/Seoul 고정)과 동일 타임존 — CI(UTC) 러너에서 시각 변환 테스트가 갈리지 않게
    systemProperty("user.timezone", "Asia/Seoul")
}
