plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "link.yologram.api.v1"
version = "0.0.1-SNAPSHOT"
description = "yologram-api-v1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JPA + MySQL
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")

    // Security (BCrypt only)
    implementation("org.springframework.security:spring-security-crypto")

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")

    // 검색 (셀프호스팅 OpenSearch) — worker와 같은 클라이언트를 쓴다.
    // httpclient5는 opensearch-java의 transport 구현이고 클라이언트가 optional 의존으로 두어 직접 선언한다.
    // 버전은 Spring Boot BOM에 맡긴다 (직접 박으면 BOM의 httpcore5-h2와 어긋나 런타임에 NoSuchMethodError)
    implementation("org.opensearch.client:opensearch-java:2.25.0")
    implementation("org.apache.httpcomponents.client5:httpclient5")

    // Observability
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.15.0-alpha")
    implementation("io.micrometer:micrometer-registry-otlp")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Spring Cloud AWS
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.0"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-parameter-store")

    // AWS SDK v2
    implementation(platform("software.amazon.awssdk:bom:2.31.62"))
    implementation("software.amazon.awssdk:ses")
    implementation("software.amazon.awssdk:kinesis")
    implementation("software.amazon.awssdk:sqs")

    // Dev
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

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
}

tasks.named<Jar>("jar") {
    enabled = false
}
