package link.yologram.api.v1.config.sqs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.time.Duration

/**
 * SQS 수동 빈 구성 — KinesisConfig 미러(리전 고정, 자격증명은 기본 체인).
 *
 * Kinesis(조회 이벤트)와 달리 SQS는 "인덱싱 작업 지시"를 넣는 용도다 —
 * 이벤트 스트림이 아니라 명령 큐라 프로퍼티 축도 yologram.messages.*로 분리했다(docs/rules.md).
 *
 * 타임아웃을 Kinesis보다 넉넉히 잡는다: 인덱싱 요청은 어드민 조작이라 사용자 응답 지연에 민감하지 않고,
 * 풀 인덱싱은 한 요청에서 수십~수백 건을 연속 발행하므로 매 건 500ms 상한은 과하게 촉박하다.
 */
@Configuration
class SqsConfig {

    @Bean
    fun sqsClient(): SqsClient {
        val overrideConfiguration = ClientOverrideConfiguration.builder()
            .apiCallTimeout(Duration.ofSeconds(5))
            .apiCallAttemptTimeout(Duration.ofSeconds(2))
            .build()

        return SqsClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .overrideConfiguration(overrideConfiguration)
            .build()
    }
}
