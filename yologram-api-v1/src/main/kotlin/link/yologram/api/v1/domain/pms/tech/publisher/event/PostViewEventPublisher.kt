package link.yologram.api.v1.domain.pms.tech.publisher.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.api.v1.config.EventPublishProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest

/**
 * 게시글 조회 이벤트 Kinesis 발행 — 도메인이 AWS SDK를 직접 모르게 infra 층에 둔다(경계).
 *
 * 조회 이벤트는 부가 기능이므로 RedisCacheService와 동일한 원칙: 전 연산 runCatching으로
 * 실패를 삼키고 warn 로그만 남긴다 (발행 실패가 상세 조회 응답을 막지 않는다).
 */
@Component
class PostViewEventPublisher(
    private val kinesisClient: KinesisClient,
    private val objectMapper: ObjectMapper,
    private val properties: EventPublishProperties,
) {

    private val logger = KotlinLogging.logger {}

    fun publish(event: PostViewEvent) {
        // 발행 비활성(로컬·테스트 기본)이면 스킵 — prod 스트림 오염 방지
        if (!properties.postView.enabled) return

        val streamName = properties.postView.stream
        if (streamName.isNullOrBlank()) {
            // 켰는데 대상이 없는 설정 실수 — 조용히 스킵하면 발행이 0건인 이유를 알 수 없다
            logger.warn { "post view event publish is enabled but stream is not configured — skipped: postId=${event.postId}" }
            return
        }

        runCatching {
            val request = PutRecordRequest.builder()
                .streamName(streamName)
                // partitionKey = postId → 같은 글의 이벤트는 같은 샤드에 들어가 순서가 보장된다
                .partitionKey(event.postId.toString())
                .data(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(event)))
                .build()

            kinesisClient.putRecord(request)
        }.onFailure {
            logger.warn(it) { "unexpected error occurred while publishing post view event: postId=${event.postId}" }
        }
    }
}
