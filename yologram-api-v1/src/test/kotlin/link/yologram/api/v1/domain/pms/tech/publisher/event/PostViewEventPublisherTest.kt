package link.yologram.api.v1.domain.pms.tech.publisher.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import link.yologram.api.v1.config.EventPublishProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import software.amazon.awssdk.services.kinesis.model.ProvisionedThroughputExceededException
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostViewEventPublisherTest {

    @Mock
    lateinit var kinesisClient: KinesisClient

    // 애플리케이션 ObjectMapper와 동일 설정 — JavaTimeModule + 날짜는 ISO 문자열(Spring Boot 기본)
    private val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json()
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()

    private fun publisher(streamName: String?, enabled: Boolean = true) = PostViewEventPublisher(
        kinesisClient,
        objectMapper,
        EventPublishProperties(postView = EventPublishProperties.Publish(enabled = enabled, stream = streamName)),
    )

    private fun capturedRequest(): PutRecordRequest {
        val captor = argumentCaptor<PutRecordRequest>()
        verify(kinesisClient).putRecord(captor.capture())
        return captor.firstValue
    }

    /** 캡처한 PutRecord의 data(JSON)를 맵으로 파싱 — 필드 구성·값 검증용 */
    private fun capturedPayload(): Map<String, Any?> =
        objectMapper.readValue(capturedRequest().data().asUtf8String(), jacksonTypeRef<Map<String, Any?>>())

    @Nested
    inner class 발행 {

        @Test
        fun `스트림 이름·partitionKey·페이로드 6개 필드로 PutRecord를 호출한다`() {
            publisher("yologram-post-view-event-test").publish(
                PostViewEvent(
                    postId = 1200L,
                    uid = 12L,
                    ip = "1.2.3.4",
                    occurredAt = LocalDateTime.of(2026, 8, 12, 21, 30, 0),
                ),
            )

            val request = capturedRequest()
            assertEquals("yologram-post-view-event-test", request.streamName())
            // partitionKey = postId 문자열 — 같은 글 이벤트는 같은 샤드(순서 보장)
            assertEquals("1200", request.partitionKey())

            val payload = objectMapper.readValue(request.data().asUtf8String(), jacksonTypeRef<Map<String, Any?>>())
            assertEquals(
                setOf("eventType", "section", "postId", "uid", "ip", "occurredAt"),
                payload.keys,
            )
            assertEquals("POST_VIEW", payload["eventType"])
            assertEquals("TECH", payload["section"])
            assertEquals(1200, payload["postId"])
            assertEquals(12, payload["uid"])
            assertEquals("1.2.3.4", payload["ip"])
            // occurredAt은 기존 직렬화 규약과 동일한 초 단위 ISO LocalDateTime
            assertEquals("2026-08-12T21:30:00", payload["occurredAt"])
        }

        @Test
        fun `비로그인 조회면 uid는 null로 발행한다 (필드 수는 동일)`() {
            publisher("yologram-post-view-event-test").publish(
                PostViewEvent(postId = 7L, uid = null, ip = "1.2.3.4"),
            )

            val payload = capturedPayload()
            assertEquals(6, payload.size)
            assertTrue(payload.containsKey("uid"))
            assertNull(payload["uid"])
        }

        @Test
        fun `IP를 못 구한 요청이면 ip는 null로 발행한다`() {
            publisher("yologram-post-view-event-test").publish(
                PostViewEvent(postId = 7L, uid = 12L, ip = null),
            )

            val payload = capturedPayload()
            assertEquals(6, payload.size)
            assertNull(payload["ip"])
        }

        @Test
        fun `occurredAt 기본값은 현재 시각의 초 단위 ISO 문자열이다`() {
            publisher("yologram-post-view-event-test").publish(
                PostViewEvent(postId = 7L, uid = null, ip = null),
            )

            val payload = capturedPayload()
            assertTrue(Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$""").matches(payload["occurredAt"] as String))
        }
    }

    @Nested
    inner class 발행_비활성 {

        @Test
        fun `enabled=false면 스트림이 있어도 PutRecord를 호출하지 않는다`() {
            // 로컬·테스트 기본값 — prod 스트림 오염 방지
            publisher("yologram-post-view-event-prod", enabled = false)
                .publish(PostViewEvent(postId = 1L, uid = null, ip = null))

            verify(kinesisClient, never()).putRecord(any<PutRecordRequest>())
        }
    }

    @Nested
    inner class 스트림_미설정 {

        @Test
        fun `스트림 이름이 null이면 PutRecord를 호출하지 않는다`() {
            publisher(null).publish(PostViewEvent(postId = 1L, uid = null, ip = null))

            verify(kinesisClient, never()).putRecord(any<PutRecordRequest>())
        }

        @Test
        fun `스트림 이름이 빈 값이면 PutRecord를 호출하지 않는다`() {
            publisher("").publish(PostViewEvent(postId = 1L, uid = null, ip = null))

            verify(kinesisClient, never()).putRecord(any<PutRecordRequest>())
        }

        @Test
        fun `스트림 이름이 공백뿐이면 PutRecord를 호출하지 않는다`() {
            publisher("   ").publish(PostViewEvent(postId = 1L, uid = null, ip = null))

            verify(kinesisClient, never()).putRecord(any<PutRecordRequest>())
        }
    }

    @Nested
    inner class 발행_실패 {

        @Test
        fun `SDK 예외가 나도 삼키고 전파하지 않는다`() {
            whenever(kinesisClient.putRecord(any<PutRecordRequest>()))
                .thenThrow(ProvisionedThroughputExceededException.builder().message("throttled").build())

            // 예외가 전파되면 상세 조회가 실패하므로 반드시 삼켜야 한다
            publisher("yologram-post-view-event-test").publish(PostViewEvent(postId = 1L, uid = null, ip = null))

            verify(kinesisClient).putRecord(any<PutRecordRequest>())
        }

        @Test
        fun `런타임 예외도 삼킨다`() {
            whenever(kinesisClient.putRecord(any<PutRecordRequest>()))
                .thenThrow(RuntimeException("boom"))

            publisher("yologram-post-view-event-test").publish(PostViewEvent(postId = 1L, uid = null, ip = null))

            verify(kinesisClient).putRecord(any<PutRecordRequest>())
        }
    }
}
