package link.yologram.api.v1.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisClient
import java.time.Duration

/**
 * Kinesis 수동 빈 구성 — 리전은 SesConfig와 동일 관례(ap-northeast-2 고정),
 * 자격증명은 기본 체인(prod: ECS Task Role, 로컬: AWS_PROFILE 환경변수).
 *
 * SesConfig(@Profile("prod"))와 달리 프로파일을 제한하지 않는다 — 발행 스킵 판단은
 * 스트림 이름(event.stream.post-view.name) 유무로만 하고(PostViewEventPublisher),
 * 빈 생성 자체는 자격증명을 요구하지 않아 로컬·테스트 부팅에 영향이 없다.
 */
@Configuration
class KinesisConfig {

    @Bean
    fun kinesisClient(): KinesisClient {
        // 조회 이벤트 발행은 부가 기능 — Kinesis 지연이 상세 조회 응답으로 전파되지 않도록 짧은 타임아웃.
        // (DB·Redis 타임아웃 교훈: 기본값은 무제한이라 장애 시 요청이 그대로 매달린다)
        // apiCallAttemptTimeout: HTTP 1회 시도, apiCallTimeout: 재시도 포함 전체 상한
        val overrideConfiguration = ClientOverrideConfiguration.builder()
            .apiCallTimeout(Duration.ofSeconds(1))
            .apiCallAttemptTimeout(Duration.ofMillis(500))
            .build()

        return KinesisClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .overrideConfiguration(overrideConfiguration)
            .build()
    }
}
