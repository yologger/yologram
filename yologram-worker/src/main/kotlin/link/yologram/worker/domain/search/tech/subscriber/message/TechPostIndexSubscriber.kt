package link.yologram.worker.domain.search.tech.subscriber.message

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.worker.domain.search.tech.service.TechPostIndexService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.model.Message

/**
 * 게시글 인덱싱 작업 소비 — 구독 진입점을 도메인 하위(subscriber/message)에 둔다.
 * 수단이 SQS라 message 하위다 (Kinesis 구독은 subscriber/event — docs/rules.md).
 *
 * 수동 ack: 색인이 성공한 뒤에만 메시지를 지운다. 실패하면 ack하지 않아 가시성 타임아웃(300초) 후
 * 재전달되고, 3회 실패하면 SQS가 DLQ로 옮긴다.
 *
 * 레거시(BoardIndexingHandler)와 다른 점: 처리를 @Async로 던지지 않는다.
 * 비동기로 던지면 리스너 메서드가 즉시 반환되어 예외가 리스너 밖에서 발생하고,
 * 그 결과 catch·가시성 조정이 동작하지 않으면서 ack 시점도 통제할 수 없다.
 * SQS 리스너는 이미 별도 스레드 풀에서 돌기 때문에 여기서 다시 비동기로 만들 이유가 없다.
 */
@Component
@ConditionalOnProperty(prefix = "yologram.messages.subscribe.post-index", name = ["enabled"], havingValue = "true")
class TechPostIndexSubscriber(
    private val objectMapper: ObjectMapper,
    private val indexService: TechPostIndexService,
) {

    private val logger = KotlinLogging.logger {}

    @SqsListener(
        value = ["\${yologram.messages.subscribe.post-index.queue}"],
        acknowledgementMode = "MANUAL",
        // 색인은 DB 조회 + bulk라 무겁다. 동시 처리를 늘리면 2GB 인스턴스의 OpenSearch 힙에 부담이 간다
        maxConcurrentMessages = "1",
        maxMessagesPerPoll = "1",
    )
    fun handle(message: Message, acknowledgement: Acknowledgement) {
        val body = message.body()

        val request = runCatching { objectMapper.readValue(body, TechPostIndexMessage::class.java) }
            .getOrElse {
                // 파싱 불가는 재시도해도 같은 결과다 — ack해서 DLQ 왕복 없이 흘려보내고 로그로 남긴다.
                // (재시도 대상은 일시적 실패이지 형식이 깨진 메시지가 아니다)
                logger.error(it) { "invalid index message — dropped: $body" }
                acknowledgement.acknowledge()
                return
            }

        if (request.target != TechPostIndexMessage.TARGET_TECH_POST) {
            logger.warn { "unsupported target — dropped: target=${request.target}" }
            acknowledgement.acknowledge()
            return
        }

        // from·to는 원시 타입이라 필드가 빠져도 파싱은 통과하고 0이 들어온다.
        // 검증하지 않으면 잘못 만든 메시지가 index(0, 0)으로 조용히 성공한 것처럼 처리된다
        if (request.from < 1 || request.to < request.from) {
            logger.warn { "invalid range — dropped: from=${request.from}, to=${request.to}" }
            acknowledgement.acknowledge()
            return
        }

        indexService.index(from = request.from, to = request.to)

        // 색인이 끝난 뒤에만 삭제 — 먼저 지우면 실패 시 그 범위가 영영 색인되지 않는다.
        // 중복 전달은 문서 id가 게시글 id로 고정되어 덮어쓰기가 되므로 무해하다
        acknowledgement.acknowledge()
    }
}
