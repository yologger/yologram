package link.yologram.worker.domain.pms.tech.service

import jakarta.persistence.EntityManager
import link.yologram.worker.domain.pms.tech.subscriber.event.PostViewEvent
import link.yologram.worker.domain.pms.tech.repository.TechPostViewCountRepository
import link.yologram.worker.domain.pms.tech.repository.TechPostViewRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechPostViewIngestServiceTest {

    @Autowired
    lateinit var ingestService: TechPostViewIngestService

    @Autowired
    lateinit var viewRepository: TechPostViewRepository

    @Autowired
    lateinit var viewCountRepository: TechPostViewCountRepository

    @Autowired
    lateinit var entityManager: EntityManager

    private val day13 = LocalDateTime.of(2026, 8, 13, 0, 10, 0)
    private val day14 = LocalDateTime.of(2026, 8, 14, 0, 10, 0)

    @BeforeEach
    fun setUp() {
        viewRepository.deleteAll()
        viewCountRepository.deleteAll()
        entityManager.flush()
    }

    private fun event(
        postId: Long = 1200,
        uid: Long? = null,
        ip: String? = null,
        occurredAt: LocalDateTime = day13,
        eventType: String = PostViewEvent.EVENT_TYPE_POST_VIEW,
    ) = PostViewEvent(
        eventType = eventType,
        section = PostViewEvent.SECTION_TECH,
        postId = postId,
        uid = uid,
        ip = ip,
        occurredAt = occurredAt,
    )

    private fun viewCountOf(postId: Long): Long? {
        entityManager.clear()
        return viewCountRepository.findByIdOrNull(postId)?.viewCount
    }

    private fun ledgerKeys(): List<String> {
        entityManager.clear()
        return viewRepository.findAll().map { it.viewKey }
    }

    @Nested
    inner class 멱등 {

        @Test
        fun `같은 레코드를 두 번 처리해도 조회 수는 1만 증가한다`() {
            val events = listOf(event(uid = 12))

            val first = ingestService.ingest(events)
            val second = ingestService.ingest(events)

            assertEquals(1, first.inserted)
            assertEquals(0, second.inserted)
            assertEquals(1L, viewCountOf(1200))
            assertEquals(1, ledgerKeys().size)
        }

        @Test
        fun `배치 안에 같은 view_key가 두 건이면 한 건만 집계한다`() {
            // 같은 유저가 같은 날 새로고침 — producer가 원본 이벤트를 2건 발행한 상황
            val result = ingestService.ingest(
                listOf(
                    event(uid = 12, occurredAt = day13),
                    event(uid = 12, occurredAt = day13.plusMinutes(5)),
                )
            )

            assertEquals(2, result.received)
            assertEquals(1, result.inserted)
            assertEquals(1L, viewCountOf(1200))
        }

        @Test
        fun `이미 적재된 키와 신규 키가 섞이면 신규만 집계한다`() {
            ingestService.ingest(listOf(event(uid = 12)))

            val result = ingestService.ingest(
                listOf(event(uid = 12), event(uid = 13))
            )

            assertEquals(1, result.inserted)
            assertEquals(2L, viewCountOf(1200))
        }
    }

    @Nested
    inner class 집계_분기 {

        @Test
        fun `같은 글 같은 유저 다른 날짜는 2건으로 집계한다 (viewDate 경계)`() {
            val result = ingestService.ingest(
                listOf(event(uid = 12, occurredAt = day13), event(uid = 12, occurredAt = day14))
            )

            assertEquals(2, result.inserted)
            assertEquals(2L, viewCountOf(1200))
            assertEquals(setOf("1200:u12:2026-08-13", "1200:u12:2026-08-14"), ledgerKeys().toSet())
        }

        @Test
        fun `같은 글 다른 유저는 2건으로 집계한다`() {
            val result = ingestService.ingest(
                listOf(event(uid = 12), event(uid = 13))
            )

            assertEquals(2, result.inserted)
            assertEquals(2L, viewCountOf(1200))
        }

        @Test
        fun `로그인과 비로그인은 서로 다른 조회로 집계한다`() {
            val result = ingestService.ingest(
                listOf(event(uid = 12), event(ip = "203.0.113.7"), event(uid = null, ip = null))
            )

            assertEquals(3, result.inserted)
            assertEquals(3L, viewCountOf(1200))
            assertEquals(
                setOf("1200:u12:2026-08-13", "1200:i203.0.113.7:2026-08-13", "1200:unknown:2026-08-13"),
                ledgerKeys().toSet(),
            )
        }

        @Test
        fun `uid와 ip가 모두 없는 이벤트는 하루 1건으로 수렴한다 (과소집계 허용)`() {
            val result = ingestService.ingest(
                listOf(
                    event(uid = null, ip = null, occurredAt = day13),
                    event(uid = null, ip = null, occurredAt = day13.plusHours(3)),
                    event(uid = null, ip = null, occurredAt = day13.plusHours(9)),
                )
            )

            assertEquals(3, result.received)
            assertEquals(1, result.inserted)
            assertEquals(1L, viewCountOf(1200))
        }

        @Test
        fun `여러 postId가 섞인 배치는 postId별 delta를 정확히 합산한다`() {
            val result = ingestService.ingest(
                listOf(
                    event(postId = 1200, uid = 1),
                    event(postId = 1200, uid = 2),
                    event(postId = 1200, uid = 2),   // 중복 — 제외
                    event(postId = 1201, uid = 1),
                    event(postId = 1202, uid = 1),
                    event(postId = 1202, uid = 2),
                    event(postId = 1202, uid = 3),
                )
            )

            assertEquals(7, result.received)
            assertEquals(6, result.inserted)
            assertEquals(3, result.updatedPostCount)
            assertEquals(2L, viewCountOf(1200))
            assertEquals(1L, viewCountOf(1201))
            assertEquals(3L, viewCountOf(1202))
        }
    }

    @Nested
    inner class 카운트_upsert {

        @Test
        fun `카운트 row가 없는 첫 이벤트는 row를 생성한다`() {
            assertNull(viewCountOf(1200))

            ingestService.ingest(listOf(event(uid = 12)))

            assertEquals(1L, viewCountOf(1200))
        }

        @Test
        fun `기존 카운트에 delta가 더해진다`() {
            ingestService.ingest(listOf(event(uid = 12, occurredAt = day13)))

            ingestService.ingest(
                listOf(event(uid = 13, occurredAt = day13), event(uid = 14, occurredAt = day13))
            )

            assertEquals(3L, viewCountOf(1200))
        }
    }

    @Nested
    inner class 빈_배치 {

        @Test
        fun `빈 배치는 아무것도 갱신하지 않는다`() {
            val result = ingestService.ingest(emptyList())

            assertEquals(0, result.received)
            assertEquals(0, result.inserted)
            assertEquals(0, result.updatedPostCount)
            assertNull(viewCountOf(1200))
        }
    }
}
