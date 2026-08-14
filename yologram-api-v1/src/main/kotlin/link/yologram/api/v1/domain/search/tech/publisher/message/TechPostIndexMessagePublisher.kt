package link.yologram.api.v1.domain.search.tech.publisher.message

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.api.v1.config.sqs.MessagePublishProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.util.concurrent.ConcurrentHashMap

/**
 * 인덱싱 작업 메시지 SQS 발행 — 발행 진입점을 도메인 하위(publisher/message)에 둔다.
 * 수단이 event(Kinesis)가 아니라 message(SQS)라 message 하위다 (docs/rules.md).
 *
 * 큐 URL은 이름으로 조회한 뒤 캐시한다(레거시 SqsClient 패턴) — 매 발행마다 GetQueueUrl을 부르지 않는다.
 *
 * 조회 이벤트 발행(PostViewEventPublisher)과 실패 처리 방침이 다르다:
 * 조회 이벤트는 사용자 응답을 막지 않아야 해서 실패를 삼키지만,
 * 인덱싱은 어드민이 명시적으로 요청한 작업이라 실패를 알려야 한다 — 예외를 전파한다.
 */
@Component
class TechPostIndexMessagePublisher(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    private val properties: MessagePublishProperties,
) {

    private val logger = KotlinLogging.logger {}
    private val queueUrlCache = ConcurrentHashMap<String, String>()

    /** 발행 가능 여부 — 호출부가 미리 확인해 불필요한 범위 계산을 건너뛸 수 있다 */
    fun isEnabled(): Boolean = properties.postIndex.enabled && !properties.postIndex.queue.isNullOrBlank()

    fun publish(message: TechPostIndexMessage) {
        if (!properties.postIndex.enabled) {
            logger.info { "post index publish disabled — skipped: from=${message.from} to=${message.to}" }
            return
        }

        val queueName = properties.postIndex.queue
        if (queueName.isNullOrBlank()) {
            // 켰는데 대상이 없는 설정 실수 — 조용히 스킵하면 인덱싱이 0건인 이유를 알 수 없다
            logger.warn { "post index publish is enabled but queue is not configured — skipped" }
            return
        }

        val request = SendMessageRequest.builder()
            .queueUrl(queueUrl(queueName))
            .messageBody(objectMapper.writeValueAsString(message))
            .build()

        sqsClient.sendMessage(request)
        logger.info { "post index message sent: target=${message.target} range=${message.from}-${message.to}" }
    }

    private fun queueUrl(queueName: String): String =
        queueUrlCache.computeIfAbsent(queueName) {
            sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(it).build()).queueUrl()
        }
}
