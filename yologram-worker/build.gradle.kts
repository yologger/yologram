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

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:junit-jupiter")
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
