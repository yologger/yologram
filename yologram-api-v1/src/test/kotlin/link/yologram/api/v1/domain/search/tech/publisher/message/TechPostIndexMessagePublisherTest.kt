package link.yologram.api.v1.domain.search.tech.publisher.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import link.yologram.api.v1.config.sqs.MessagePublishProperties
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SqsException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TechPostIndexMessagePublisherTest {

    private val sqsClient = mock<SqsClient>()
    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    private val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/yologram-search-indexing-prod"

    private fun publisher(
        enabled: Boolean = true,
        queue: String? = "yologram-search-indexing-prod",
    ): TechPostIndexMessagePublisher {
        val properties = MessagePublishProperties(
            postIndex = MessagePublishProperties.Publish(enabled = enabled, queue = queue),
        )
        return TechPostIndexMessagePublisher(sqsClient, objectMapper, properties)
    }

    private fun givenQueueUrl() {
        whenever(sqsClient.getQueueUrl(any<GetQueueUrlRequest>()))
            .thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build())
    }

    private fun sentBody(): String {
        val captor = argumentCaptor<SendMessageRequest>()
        verify(sqsClient).sendMessage(captor.capture())
        return captor.firstValue.messageBody()
    }

    @Nested
    inner class 발행 {

        @Test
        fun `조회한 큐 URL로 메시지를 보낸다`() {
            givenQueueUrl()

            publisher().publish(TechPostIndexMessage(from = 1, to = 20))

            val captor = argumentCaptor<SendMessageRequest>()
            verify(sqsClient).sendMessage(captor.capture())
            assertEquals(queueUrl, captor.firstValue.queueUrl())
        }

        @Test
        fun `본문은 target과 범위를 담은 JSON이다`() {
            givenQueueUrl()

            publisher().publish(TechPostIndexMessage(from = 1, to = 20))

            // 워커가 이 세 필드로 역직렬화한다 — 이름이 바뀌면 소비가 깨진다
            val body: Map<String, Any> = objectMapper.readValue(sentBody())
            assertEquals("TECH_POST", body["target"])
            assertEquals(1, body["from"])
            assertEquals(20, body["to"])
        }

        @Test
        fun `큐 URL은 한 번만 조회하고 재사용한다`() {
            givenQueueUrl()
            val publisher = publisher()

            publisher.publish(TechPostIndexMessage(from = 1, to = 20))
            publisher.publish(TechPostIndexMessage(from = 21, to = 40))

            verify(sqsClient, times(1)).getQueueUrl(any<GetQueueUrlRequest>())
            verify(sqsClient, times(2)).sendMessage(any<SendMessageRequest>())
        }
    }

    @Nested
    inner class 스킵 {

        @Test
        fun `비활성이면 SQS를 호출하지 않는다`() {
            publisher(enabled = false).publish(TechPostIndexMessage(from = 1, to = 20))

            verify(sqsClient, never()).getQueueUrl(any<GetQueueUrlRequest>())
            verify(sqsClient, never()).sendMessage(any<SendMessageRequest>())
        }

        @Test
        fun `활성인데 큐 이름이 없으면 보내지 않는다`() {
            // 설정 실수 — 예외 없이 스킵하되 경고를 남긴다
            publisher(queue = null).publish(TechPostIndexMessage(from = 1, to = 20))

            verify(sqsClient, never()).sendMessage(any<SendMessageRequest>())
        }

        @Test
        fun `큐 이름이 공백이면 보내지 않는다`() {
            publisher(queue = "   ").publish(TechPostIndexMessage(from = 1, to = 20))

            verify(sqsClient, never()).sendMessage(any<SendMessageRequest>())
        }
    }

    @Nested
    inner class 활성_여부 {

        @Test
        fun `활성이고 큐가 있으면 true`() {
            assertTrue(publisher().isEnabled())
        }

        @Test
        fun `비활성이면 false`() {
            assertFalse(publisher(enabled = false).isEnabled())
        }

        @Test
        fun `큐가 없으면 false`() {
            assertFalse(publisher(queue = null).isEnabled())
        }
    }

    @Nested
    inner class 실패 {

        @Test
        fun `전송 실패는 삼키지 않고 전파한다`() {
            // 조회 이벤트 발행과 다르다 — 어드민이 명시 요청한 작업이라 실패를 알려야 한다
            givenQueueUrl()
            whenever(sqsClient.sendMessage(any<SendMessageRequest>()))
                .thenThrow(SqsException.builder().message("queue does not exist").build())

            assertFailsWith<SqsException> {
                publisher().publish(TechPostIndexMessage(from = 1, to = 20))
            }
        }

        @Test
        fun `큐 URL 조회 실패도 전파한다`() {
            whenever(sqsClient.getQueueUrl(any<GetQueueUrlRequest>()))
                .thenThrow(SqsException.builder().message("non-existent queue").build())

            assertFailsWith<SqsException> {
                publisher().publish(TechPostIndexMessage(from = 1, to = 20))
            }

            verify(sqsClient, never()).sendMessage(any<SendMessageRequest>())
        }
    }
}
