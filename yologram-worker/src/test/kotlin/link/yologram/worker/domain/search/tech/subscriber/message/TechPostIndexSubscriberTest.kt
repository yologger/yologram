package link.yologram.worker.domain.search.tech.subscriber.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import link.yologram.worker.domain.search.tech.service.TechPostIndexService
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.model.Message
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TechPostIndexSubscriberTest {

    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    private val indexService = mock<TechPostIndexService>()
    private val acknowledgement = mock<Acknowledgement>()

    private val subscriber = TechPostIndexSubscriber(objectMapper, indexService)

    private fun message(body: String): Message = Message.builder().body(body).build()

    private fun json(target: String = "TECH_POST", from: Long = 1, to: Long = 20) =
        """{"target":"$target","from":$from,"to":$to}"""

    @Nested
    inner class 정상 {

        @Test
        fun `범위 메시지를 그대로 색인 서비스에 넘기고 ack한다`() {
            subscriber.handle(message(json(from = 1, to = 20)), acknowledgement)

            verify(indexService).index(from = 1, to = 20)
            verify(acknowledgement).acknowledge()
        }

        @Test
        fun `단건 인덱싱은 from과 to가 같은 범위로 온다`() {
            subscriber.handle(message(json(from = 77, to = 77)), acknowledgement)

            verify(indexService).index(from = 77, to = 77)
            verify(acknowledgement).acknowledge()
        }

        @Test
        fun `발행 쪽이 필드를 추가해도 무시하고 처리한다`() {
            // 발행자(api-v1)를 먼저 배포해도 소비가 깨지지 않아야 한다
            val body = """{"target":"TECH_POST","from":1,"to":5,"requestedBy":"admin","priority":"HIGH"}"""

            subscriber.handle(message(body), acknowledgement)

            verify(indexService).index(from = 1, to = 5)
            verify(acknowledgement).acknowledge()
        }
    }

    @Nested
    inner class 버리는_메시지 {

        @Test
        fun `JSON이 깨졌으면 색인하지 않고 ack해서 흘려보낸다`() {
            // 재시도해도 결과가 같으므로 DLQ 왕복 없이 버린다
            subscriber.handle(message("{not-json"), acknowledgement)

            verify(indexService, never()).index(any(), any())
            verify(acknowledgement).acknowledge()
        }

        @Test
        fun `from과 to가 빠졌으면 색인하지 않고 ack한다`() {
            // 원시 타입이라 파싱은 통과하고 0이 들어온다 — 범위 검증이 없으면 index(0, 0)이 돈다
            subscriber.handle(message("""{"target":"TECH_POST"}"""), acknowledgement)

            verify(indexService, never()).index(any(), any())
            verify(acknowledgement).acknowledge()
        }

        @Test
        fun `from이 to보다 크면 색인하지 않고 ack한다`() {
            subscriber.handle(message(json(from = 30, to = 10)), acknowledgement)

            verify(indexService, never()).index(any(), any())
            verify(acknowledgement).acknowledge()
        }

        @Test
        fun `id는 1부터라 0 이하 범위는 색인하지 않는다`() {
            subscriber.handle(message(json(from = 0, to = 10)), acknowledgement)

            verify(indexService, never()).index(any(), any())
            verify(acknowledgement).acknowledge()
        }

        @Test
        fun `지원하지 않는 target이면 색인하지 않고 ack한다`() {
            // 같은 큐를 다른 대상(USER 등)이 쓰게 되기 전까지는 흘려보낸다
            subscriber.handle(message(json(target = "USER")), acknowledgement)

            verify(indexService, never()).index(any(), any())
            verify(acknowledgement).acknowledge()
        }
    }

    @Nested
    inner class 실패 {

        @Test
        fun `색인이 실패하면 ack하지 않아 재전달된다`() {
            whenever(indexService.index(any(), any())).thenThrow(RuntimeException("opensearch down"))

            assertFailsWith<RuntimeException> {
                subscriber.handle(message(json()), acknowledgement)
            }

            // ack하지 않아야 가시성 타임아웃 후 재전달되고, 3회 실패 시 DLQ로 간다
            verify(acknowledgement, never()).acknowledge()
        }

        @Test
        fun `색인 도중 실패해도 메시지를 삭제하지 않는다`() {
            whenever(indexService.index(eq(1L), eq(20L))).thenThrow(IllegalStateException("bulk rejected"))

            assertFailsWith<IllegalStateException> {
                subscriber.handle(message(json(from = 1, to = 20)), acknowledgement)
            }

            verify(acknowledgement, never()).acknowledge()
        }
    }

    @Nested
    inner class 계약 {

        @Test
        fun `target 상수는 발행자와 같은 문자열이다`() {
            // api-v1 TechPostIndexMessage.TARGET_TECH_POST와 문자열로만 맞물린다 (모듈 공유 없음)
            assertEquals("TECH_POST", TechPostIndexMessage.TARGET_TECH_POST)
        }
    }
}
