package link.yologram.api.v1.domain.search.tech.service

import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import link.yologram.api.v1.domain.search.exception.InvalidIndexRangeException
import link.yologram.api.v1.domain.search.tech.publisher.message.TechPostIndexMessage
import link.yologram.api.v1.domain.search.tech.publisher.message.TechPostIndexMessagePublisher
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminTechPostIndexingServiceTest {

    private val postRepository = mock<TechPostRepository>()
    private val publisher = mock<TechPostIndexMessagePublisher>()

    private val service = AdminTechPostIndexingService(postRepository, publisher)

    private fun publishedMessages(): List<TechPostIndexMessage> {
        val captor = argumentCaptor<TechPostIndexMessage>()
        verify(publisher, org.mockito.kotlin.atLeastOnce()).publish(captor.capture())
        return captor.allValues
    }

    @Nested
    inner class 단건 {

        @Test
        fun `from과 to를 같은 값으로 발행한다`() {
            service.index(id = 42)

            val messages = publishedMessages()
            assertEquals(1, messages.size)
            assertEquals(42L, messages[0].from)
            assertEquals(42L, messages[0].to)
            assertEquals(TechPostIndexMessage.TARGET_TECH_POST, messages[0].target)
        }
    }

    @Nested
    inner class 범위 {

        @Test
        fun `청크 크기 이하 범위는 한 건만 발행한다`() {
            val published = service.index(from = 1, to = 20)

            assertEquals(1, published)
            assertEquals(listOf(1L to 20L), publishedMessages().map { it.from to it.to })
        }

        @Test
        fun `청크 경계를 넘으면 나눠 발행한다`() {
            val published = service.index(from = 1, to = 45)

            assertEquals(3, published)
            // 마지막 청크는 to에서 끊긴다 — 범위 밖 id를 조회하지 않는다
            assertEquals(
                listOf(1L to 20L, 21L to 40L, 41L to 45L),
                publishedMessages().map { it.from to it.to },
            )
        }

        @Test
        fun `청크로 정확히 나눠떨어지면 빈 메시지를 더 만들지 않는다`() {
            val published = service.index(from = 1, to = 40)

            assertEquals(2, published)
            assertEquals(listOf(1L to 20L, 21L to 40L), publishedMessages().map { it.from to it.to })
        }

        @Test
        fun `from과 to가 같으면 한 건만 발행한다`() {
            val published = service.index(from = 7, to = 7)

            assertEquals(1, published)
            assertEquals(listOf(7L to 7L), publishedMessages().map { it.from to it.to })
        }

        @Test
        fun `1에서 시작하지 않는 범위도 그대로 쪼갠다`() {
            val published = service.index(from = 100, to = 130)

            assertEquals(2, published)
            assertEquals(listOf(100L to 119L, 120L to 130L), publishedMessages().map { it.from to it.to })
        }

        @Test
        fun `from이 to보다 크면 발행하지 않고 예외를 던진다`() {
            assertFailsWith<InvalidIndexRangeException> { service.index(from = 30, to = 10) }

            verify(publisher, never()).publish(org.mockito.kotlin.any())
        }

        @Test
        fun `id는 1부터라 0 이하로 시작하는 범위는 예외를 던진다`() {
            assertFailsWith<InvalidIndexRangeException> { service.index(from = 0, to = 10) }

            verify(publisher, never()).publish(org.mockito.kotlin.any())
        }
    }

    @Nested
    inner class 전체 {

        @Test
        fun `1부터 max id까지 쪼개 발행한다`() {
            whenever(postRepository.findMaxId()).thenReturn(45L)

            val published = service.fullIndex()

            assertEquals(3, published)
            assertEquals(
                listOf(1L to 20L, 21L to 40L, 41L to 45L),
                publishedMessages().map { it.from to it.to },
            )
        }

        @Test
        fun `글이 하나도 없으면 발행하지 않는다`() {
            whenever(postRepository.findMaxId()).thenReturn(null)

            val published = service.fullIndex()

            assertEquals(0, published)
            verify(publisher, never()).publish(org.mockito.kotlin.any())
        }

        @Test
        fun `max id가 0이면 발행하지 않는다`() {
            whenever(postRepository.findMaxId()).thenReturn(0L)

            val published = service.fullIndex()

            assertEquals(0, published)
            verify(publisher, never()).publish(org.mockito.kotlin.any())
        }

        @Test
        fun `글이 하나뿐이면 한 건만 발행한다`() {
            whenever(postRepository.findMaxId()).thenReturn(1L)

            val published = service.fullIndex()

            assertEquals(1, published)
            assertEquals(listOf(1L to 1L), publishedMessages().map { it.from to it.to })
        }

        @Test
        fun `비동기 진입점도 같은 범위를 발행한다`() {
            whenever(postRepository.findMaxId()).thenReturn(45L)

            service.fullIndexAsync()

            assertEquals(
                listOf(1L to 20L, 21L to 40L, 41L to 45L),
                publishedMessages().map { it.from to it.to },
            )
        }

        @Test
        fun `비동기 진입점은 발행 실패를 삼킨다`() {
            // @Async라 예외를 호출자에게 전달할 수 없다 — 밖으로 던지면 기본 핸들러 로그로만 남는다
            whenever(postRepository.findMaxId()).thenReturn(45L)
            whenever(publisher.publish(org.mockito.kotlin.any())).thenThrow(RuntimeException("sqs down"))

            service.fullIndexAsync()
        }

        @Test
        fun `동기 fullIndex는 실패를 전파한다`() {
            // 비동기 래퍼만 삼킨다 — 안쪽은 그대로 두어 다른 호출자가 실패를 알 수 있게 한다
            whenever(postRepository.findMaxId()).thenReturn(45L)
            whenever(publisher.publish(org.mockito.kotlin.any())).thenThrow(RuntimeException("sqs down"))

            assertFailsWith<RuntimeException> { service.fullIndex() }
        }

        @Test
        fun `삭제로 id에 공백이 있어도 max id까지 전부 훑는다`() {
            // 삭제된 id 구간은 워커가 조회 0건으로 흘려보낸다 — 발행 단계에서 걸러내지 않는다
            whenever(postRepository.findMaxId()).thenReturn(21L)

            val published = service.fullIndex()

            assertEquals(2, published)
            assertEquals(listOf(1L to 20L, 21L to 21L), publishedMessages().map { it.from to it.to })
        }
    }
}
