package link.yologram.worker.domain.pms.tech.subscriber.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import link.yologram.worker.domain.pms.tech.service.TechPostViewIngestService
import org.springframework.integration.aws.inbound.kinesis.Checkpointer
import org.springframework.integration.aws.support.AwsHeaders
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import software.amazon.awssdk.utils.BinaryUtils
import software.amazon.kinesis.processor.RecordProcessorCheckpointer
import software.amazon.kinesis.retrieval.KinesisClientRecord
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

@Component
class PostViewEventSubscriber(
    private val objectMapper: ObjectMapper,
    private val techPostViewIngestService: TechPostViewIngestService,
) {

    fun handle(message: Message<*>) {
        // listener-mode=batch면 페이로드가 레코드 리스트.
        val records = when (val payload = message.payload) {
            is List<*> -> payload
            else -> listOf(payload)
        }
        val events = records.mapNotNull { parse(it) }

        // 집계는 트랜잭션 — 이력 INSERT + 카운트 UPDATE가 함께 커밋된다
        if (events.isNotEmpty()) {
            techPostViewIngestService.ingest(events)
        } else if (records.isNotEmpty()) {
            logger.warn { "게시글 조회 이벤트 배치가 전부 스킵됨: records=${records.size}" }
        }

        // 체크포인트는 반드시 커밋 이후 — 먼저 찍으면 유실, 나중에 찍으면 중복이고 중복은 uk가 흡수한다
        checkpoint(message)
    }

    /**
     * 레코드 1건 파싱. 포이즌 레코드(깨진 JSON·타입 불일치·다른 eventType·다른 section)는
     * 예외를 전파하지 않고 warn 로그 + null(스킵)로 처리한다 —
     * 예외를 올리면 그 배치가 체크포인트되지 못해 같은 레코드를 영구히 재처리하며 소비가 멈춘다.
     */
    private fun parse(record: Any?): PostViewEvent? {
        val body = bodyOf(record)
        if (body == null) {
            logger.warn { "게시글 조회 이벤트 레코드 형식을 해석할 수 없음: type=${record?.javaClass?.name}" }
            return null
        }

        val event = runCatching { objectMapper.readValue(body, PostViewEvent::class.java) }
            .onFailure { logger.warn(it) { "게시글 조회 이벤트 파싱 실패 — 스킵: body=${body.decodeToString()}" } }
            .getOrNull()
            ?: return null

        if (event.eventType != PostViewEvent.EVENT_TYPE_POST_VIEW) {
            logger.warn { "지원하지 않는 eventType — 스킵: eventType=${event.eventType} postId=${event.postId}" }
            return null
        }
        if (event.section != PostViewEvent.SECTION_TECH) {
            // 섹션별 테이블 구조라 TECH 외 섹션은 그 섹션 이력이 생길 때 분기한다
            logger.warn { "지원하지 않는 section — 스킵: section=${event.section} postId=${event.postId}" }
            return null
        }
        if (event.postId <= 0) {
            // postId가 누락된 JSON은 파싱 예외가 아니라 0으로 채워진다 —
            // Long이 JVM 프리미티브라 jackson-module-kotlin의 필수 파라미터 검사가 걸리지 않는다.
            // 막지 않으면 tech_post_view_count에 post_id=0 쓰레기 row가 생긴다
            logger.warn { "postId가 없거나 유효하지 않음 — 스킵: postId=${event.postId}" }
            return null
        }
        return event
    }

    /**
     * 원소에서 레코드 원본 바이트를 꺼낸다. KCL 모드는 KinesisClientRecord(data는 ByteBuffer),
     * 기본 모드는 Message<ByteArray>로 오고, 원본 바이트/문자열이 직접 오는 경우도 함께 받아준다.
     * ByteBuffer는 position을 건드리면 재처리 시 빈 바이트가 되므로 BinaryUtils로 비파괴 복사한다.
     */
    private fun bodyOf(record: Any?): ByteArray? = when (record) {
        is KinesisClientRecord -> BinaryUtils.copyAllBytesFrom(record.data())
        is Message<*> -> bodyOf(record.payload)
        is ByteArray -> record
        is ByteBuffer -> BinaryUtils.copyAllBytesFrom(record)
        is String -> record.toByteArray()
        else -> null
    }

    /**
     * 수동 체크포인트. 헤더에 담기는 체크포인터 타입이 바인더 모드마다 다르다 —
     * KCL 모드는 KCL의 RecordProcessorCheckpointer(반환값 없음)를 그대로 넣고,
     * 기본 모드는 spring-integration-aws의 Checkpointer(반영 여부 boolean)를 넣는다. 양쪽 다 받는다.
     */
    private fun checkpoint(message: Message<*>) {
        val result = when (val checkpointer = message.headers[AwsHeaders.CHECKPOINTER]) {
            is RecordProcessorCheckpointer -> runCatching { checkpointer.checkpoint(); true }
            is Checkpointer -> runCatching { checkpointer.checkpoint() }
            else -> {
                // checkpoint-mode=manual이 아니면 바인더가 알아서 찍는다 (단위 테스트에서도 헤더 없음)
                logger.debug { "체크포인터 헤더 없음 — 수동 체크포인트 스킵" }
                return
            }
        }

        result
            .onSuccess { if (!it) logger.warn { "게시글 조회 이벤트 체크포인트가 반영되지 않음 (이미 상위 시퀀스가 기록됨)" } }
            .onFailure {
                // 체크포인트 실패는 재처리로 이어지고 중복은 uk가 흡수하므로 배치를 실패시키지 않는다
                logger.warn(it) { "게시글 조회 이벤트 체크포인트 실패 — 다음 회차 재처리로 수렴" }
            }
    }
}
