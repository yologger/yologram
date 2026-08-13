package link.yologram.worker.domain.pms.tech.subscriber.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import link.yologram.worker.domain.pms.tech.service.TechPostViewIngestService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.integration.aws.inbound.kinesis.Checkpointer
import org.springframework.integration.aws.support.AwsHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.GenericMessage
import software.amazon.kinesis.processor.RecordProcessorCheckpointer
import software.amazon.kinesis.retrieval.KinesisClientRecord
import java.nio.ByteBuffer
import java.time.LocalDateTime
import kotlin.test.assertEquals

class PostViewEventSubscriberTest {

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    private val ingestService = mock<TechPostViewIngestService>()

    private val subscriber = PostViewEventSubscriber(objectMapper, ingestService)

    /** 기본(샤드 폴링) 모드가 넘기는 원소 형태 */
    private fun record(json: String): Message<ByteArray> = GenericMessage(json.toByteArray())

    /** KCL 모드가 넘기는 원소 형태 — data는 ByteBuffer다 */
    private fun kclRecord(json: String): KinesisClientRecord = KinesisClientRecord.builder()
        .partitionKey("1200")
        .sequenceNumber("49590338271490256608559692538361571095921575989136588898")
        .data(ByteBuffer.wrap(json.toByteArray()))
        .build()

    private fun json(
        postId: Long = 1200,
        uid: String = "null",
        ip: String = "\"203.0.113.7\"",
        occurredAt: String = "2026-08-13T00:10:00",
        eventType: String = "POST_VIEW",
        section: String = "TECH",
    ) = """{"eventType":"$eventType","section":"$section","postId":$postId,"uid":$uid,"ip":$ip,"occurredAt":"$occurredAt"}"""

    private fun message(vararg records: Any?, checkpointer: Any? = null): Message<*> {
        val headers = checkpointer?.let { mapOf(AwsHeaders.CHECKPOINTER to it) } ?: emptyMap()
        return GenericMessage(records.toList(), headers)
    }

    private fun capturedEvents(): List<PostViewEvent> {
        val captor = argumentCaptor<List<PostViewEvent>>()
        verify(ingestService).ingest(captor.capture())
        return captor.firstValue
    }

    @Nested
    inner class 정상_파싱 {

        @Test
        fun `배치 레코드를 이벤트로 변환해 집계에 넘긴다`() {
            subscriber.handle(message(record(json(postId = 1200)), record(json(postId = 1201))))

            val events = capturedEvents()
            assertEquals(listOf(1200L, 1201L), events.map { it.postId })
            assertEquals(LocalDateTime.of(2026, 8, 13, 0, 10, 0), events.first().occurredAt)
            assertEquals("203.0.113.7", events.first().ip)
        }

        @Test
        fun `uid가 null인 비로그인 이벤트도 파싱한다`() {
            subscriber.handle(message(record(json(uid = "null"))))

            assertEquals(listOf<Long?>(null), capturedEvents().map { it.uid })
        }

        @Test
        fun `uid가 있는 로그인 이벤트를 파싱한다`() {
            subscriber.handle(message(record(json(uid = "12"))))

            assertEquals(listOf<Long?>(12L), capturedEvents().map { it.uid })
        }

        @Test
        fun `ip가 null인 이벤트도 파싱한다`() {
            subscriber.handle(message(record(json(ip = "null"))))

            assertEquals(listOf<String?>(null), capturedEvents().map { it.ip })
        }

        @Test
        fun `IPv6 ip를 파싱한다`() {
            subscriber.handle(message(record(json(ip = "\"0:0:0:0:0:0:0:1\""))))

            assertEquals(listOf("0:0:0:0:0:0:0:1"), capturedEvents().map { it.ip })
        }

        @Test
        fun `계약에 없는 필드가 추가돼도 무시하고 파싱한다`() {
            val withExtra = """{"eventType":"POST_VIEW","section":"TECH","postId":1200,"uid":null,""" +
                """"ip":null,"occurredAt":"2026-08-13T00:10:00","newField":"x"}"""

            subscriber.handle(message(record(withExtra)))

            assertEquals(listOf(1200L), capturedEvents().map { it.postId })
        }

        @Test
        fun `레코드가 ByteArray나 String으로 직접 와도 파싱한다`() {
            subscriber.handle(message(json(postId = 1200).toByteArray(), json(postId = 1201)))

            assertEquals(listOf(1200L, 1201L), capturedEvents().map { it.postId })
        }

        @Test
        fun `KCL 모드의 KinesisClientRecord 배치를 파싱한다`() {
            subscriber.handle(message(kclRecord(json(postId = 1200)), kclRecord(json(postId = 1201))))

            assertEquals(listOf(1200L, 1201L), capturedEvents().map { it.postId })
        }

        @Test
        fun `KinesisClientRecord의 ByteBuffer를 소진하지 않아 같은 레코드를 다시 처리해도 파싱된다`() {
            // BinaryUtils 비파괴 복사 계약 — position을 소진하면 재처리 시 빈 바이트가 되어 조용히 스킵된다
            val record = kclRecord(json(postId = 1200))

            subscriber.handle(message(record))
            subscriber.handle(message(record))

            val captor = argumentCaptor<List<PostViewEvent>>()
            verify(ingestService, times(2)).ingest(captor.capture())
            assertEquals(listOf(1200L, 1200L), captor.allValues.flatten().map { it.postId })
        }

        @Test
        fun `ByteBuffer가 직접 와도 파싱한다`() {
            subscriber.handle(message(ByteBuffer.wrap(json(postId = 1200).toByteArray())))

            assertEquals(listOf(1200L), capturedEvents().map { it.postId })
        }
    }

    @Nested
    inner class 포이즌_레코드 {

        @Test
        fun `깨진 JSON은 스킵하고 나머지는 정상 처리한다`() {
            subscriber.handle(message(record("{not-json"), record(json(postId = 1201))))

            assertEquals(listOf(1201L), capturedEvents().map { it.postId })
        }

        @Test
        fun `다른 eventType은 스킵한다`() {
            subscriber.handle(message(record(json(eventType = "POST_LIKE")), record(json(postId = 1201))))

            assertEquals(listOf(1201L), capturedEvents().map { it.postId })
        }

        @Test
        fun `다른 section은 스킵한다`() {
            subscriber.handle(message(record(json(section = "INVEST")), record(json(postId = 1201))))

            assertEquals(listOf(1201L), capturedEvents().map { it.postId })
        }

        @Test
        fun `postId가 없는 레코드는 스킵한다`() {
            val missingPostId = """{"eventType":"POST_VIEW","section":"TECH","uid":null,"ip":null,""" +
                """"occurredAt":"2026-08-13T00:10:00"}"""

            subscriber.handle(message(record(missingPostId), record(json(postId = 1201))))

            assertEquals(listOf(1201L), capturedEvents().map { it.postId })
        }

        @Test
        fun `occurredAt 형식이 깨진 레코드는 스킵한다`() {
            subscriber.handle(message(record(json(occurredAt = "13-08-2026")), record(json(postId = 1201))))

            assertEquals(listOf(1201L), capturedEvents().map { it.postId })
        }

        @Test
        fun `해석할 수 없는 원소 타입은 스킵한다`() {
            subscriber.handle(message(42, record(json(postId = 1201))))

            assertEquals(listOf(1201L), capturedEvents().map { it.postId })
        }

        @Test
        fun `전부 포이즌이면 집계를 호출하지 않고 예외도 던지지 않는다`() {
            subscriber.handle(message(record("{not-json"), record(json(eventType = "OTHER"))))

            verify(ingestService, never()).ingest(any())
        }

        @Test
        fun `빈 배치는 집계를 호출하지 않는다`() {
            subscriber.handle(message())

            verify(ingestService, never()).ingest(any())
        }
    }

    @Nested
    inner class 체크포인트 {

        @Test
        fun `집계 커밋 후에 체크포인트를 찍는다`() {
            val checkpointer = mock<Checkpointer> { on { checkpoint() } doReturn true }

            subscriber.handle(message(record(json()), checkpointer = checkpointer))

            inOrder(ingestService, checkpointer) {
                verify(ingestService).ingest(any())
                verify(checkpointer).checkpoint()
            }
        }

        @Test
        fun `포이즌만 있는 배치도 체크포인트를 찍는다 (무한 재처리 방지)`() {
            val checkpointer = mock<Checkpointer> { on { checkpoint() } doReturn true }

            subscriber.handle(message(record("{not-json"), checkpointer = checkpointer))

            verify(ingestService, never()).ingest(any())
            verify(checkpointer).checkpoint()
        }

        @Test
        fun `체크포인터 헤더가 없으면 수동 체크포인트를 건너뛴다`() {
            subscriber.handle(message(record(json())))

            verify(ingestService).ingest(any())
        }

        @Test
        fun `체크포인트 실패는 배치를 실패시키지 않는다 (재처리로 수렴)`() {
            val checkpointer = mock<Checkpointer>()
            whenever(checkpointer.checkpoint()).thenThrow(RuntimeException("dynamodb down"))

            subscriber.handle(message(record(json()), checkpointer = checkpointer))

            verify(ingestService).ingest(any())
        }

        @Test
        fun `체크포인트가 반영되지 않아도(false) 예외를 던지지 않는다`() {
            val checkpointer = mock<Checkpointer> { on { checkpoint() } doReturn false }

            subscriber.handle(message(record(json()), checkpointer = checkpointer))

            verify(checkpointer).checkpoint()
        }

        @Test
        fun `KCL 모드 체크포인터(RecordProcessorCheckpointer)도 집계 커밋 후에 호출한다`() {
            // KCL 모드는 spring-integration-aws의 Checkpointer가 아니라 KCL 타입이 헤더로 온다 —
            // 한쪽 타입만 캐스팅하면 체크포인트가 조용히 스킵돼 재기동마다 전체 재처리가 된다
            val checkpointer = mock<RecordProcessorCheckpointer>()

            subscriber.handle(message(kclRecord(json()), checkpointer = checkpointer))

            inOrder(ingestService, checkpointer) {
                verify(ingestService).ingest(any())
                verify(checkpointer).checkpoint()
            }
        }

        @Test
        fun `KCL 체크포인터가 포이즌만 있는 배치에서도 호출된다`() {
            val checkpointer = mock<RecordProcessorCheckpointer>()

            subscriber.handle(message(kclRecord("{not-json"), checkpointer = checkpointer))

            verify(ingestService, never()).ingest(any())
            verify(checkpointer).checkpoint()
        }

        @Test
        fun `KCL 체크포인터 실패도 배치를 실패시키지 않는다`() {
            val checkpointer = mock<RecordProcessorCheckpointer>()
            whenever(checkpointer.checkpoint()).thenThrow(RuntimeException("lease lost"))

            subscriber.handle(message(kclRecord(json()), checkpointer = checkpointer))

            verify(ingestService).ingest(any())
        }

        @Test
        fun `알 수 없는 체크포인터 타입은 스킵하고 집계는 정상 처리한다`() {
            subscriber.handle(message(record(json()), checkpointer = "not-a-checkpointer"))

            verify(ingestService).ingest(any())
        }
    }
}
