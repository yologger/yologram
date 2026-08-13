package link.yologram.worker.domain.pms.tech.service

import link.yologram.worker.domain.pms.tech.subscriber.event.PostViewEvent
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PostViewKeyFactoryTest {

    private fun event(
        postId: Long = 1200,
        uid: Long? = null,
        ip: String? = null,
        occurredAt: LocalDateTime = LocalDateTime.of(2026, 8, 13, 0, 10, 0),
    ) = PostViewEvent(
        eventType = PostViewEvent.EVENT_TYPE_POST_VIEW,
        section = PostViewEvent.SECTION_TECH,
        postId = postId,
        uid = uid,
        ip = ip,
        occurredAt = occurredAt,
    )

    @Nested
    inner class viewer_분기 {

        @Test
        fun `로그인 유저는 uid로 키를 만든다`() {
            assertEquals("1200:u12:2026-08-13", PostViewKeyFactory.create(event(uid = 12, ip = "203.0.113.7")))
        }

        @Test
        fun `비로그인은 ip로 키를 만든다`() {
            assertEquals("1200:i203.0.113.7:2026-08-13", PostViewKeyFactory.create(event(ip = "203.0.113.7")))
        }

        @Test
        fun `uid가 있으면 ip보다 우선한다`() {
            val byUid = PostViewKeyFactory.create(event(uid = 12, ip = "203.0.113.7"))
            val byIp = PostViewKeyFactory.create(event(ip = "203.0.113.7"))

            assertEquals("1200:u12:2026-08-13", byUid)
            assertNotEquals(byIp, byUid)
        }

        @Test
        fun `uid와 ip가 모두 없으면 unknown으로 수렴한다`() {
            assertEquals("1200:unknown:2026-08-13", PostViewKeyFactory.create(event()))
        }

        @Test
        fun `ip가 빈 문자열이면 unknown으로 취급한다`() {
            assertEquals("1200:unknown:2026-08-13", PostViewKeyFactory.create(event(ip = "")))
            assertEquals("1200:unknown:2026-08-13", PostViewKeyFactory.create(event(ip = "   ")))
        }

        @Test
        fun `IPv6 완전 표기도 키에 그대로 들어간다`() {
            val key = PostViewKeyFactory.create(event(ip = "0:0:0:0:0:0:0:1"))

            assertEquals("1200:i0:0:0:0:0:0:0:1:2026-08-13", key)
            // varchar(120) 안에 들어간다 (IPv6 최대 45자 + postId + 날짜)
            assert(key.length <= 120)
        }
    }

    @Nested
    inner class 조회_날짜 {

        @Test
        fun `같은 날 다른 시각은 같은 키가 된다`() {
            val morning = PostViewKeyFactory.create(event(uid = 12, occurredAt = LocalDateTime.of(2026, 8, 13, 0, 0, 0)))
            val night = PostViewKeyFactory.create(event(uid = 12, occurredAt = LocalDateTime.of(2026, 8, 13, 23, 59, 59)))

            assertEquals(morning, night)
        }

        @Test
        fun `날짜가 넘어가면 다른 키가 된다 (viewDate 경계)`() {
            val before = PostViewKeyFactory.create(event(uid = 12, occurredAt = LocalDateTime.of(2026, 8, 13, 23, 59, 59)))
            val after = PostViewKeyFactory.create(event(uid = 12, occurredAt = LocalDateTime.of(2026, 8, 14, 0, 0, 0)))

            assertEquals("1200:u12:2026-08-13", before)
            assertEquals("1200:u12:2026-08-14", after)
        }

        @Test
        fun `viewDate는 처리 시각이 아니라 occurredAt 기준이다`() {
            // 같은 이벤트를 언제 처리하든(재전달·재기동) 키가 변하지 않아야 멱등이 유지된다
            val occurredAt = LocalDateTime.of(2026, 8, 13, 0, 10, 0)

            val first = PostViewKeyFactory.create(event(uid = 12, occurredAt = occurredAt))
            val reprocessed = PostViewKeyFactory.create(event(uid = 12, occurredAt = occurredAt))

            assertEquals(first, reprocessed)
            assertEquals("1200:u12:2026-08-13", first)
        }
    }

    @Test
    fun `postId가 다르면 다른 키가 된다`() {
        assertEquals("1200:u12:2026-08-13", PostViewKeyFactory.create(event(postId = 1200, uid = 12)))
        assertEquals("1201:u12:2026-08-13", PostViewKeyFactory.create(event(postId = 1201, uid = 12)))
    }
}
