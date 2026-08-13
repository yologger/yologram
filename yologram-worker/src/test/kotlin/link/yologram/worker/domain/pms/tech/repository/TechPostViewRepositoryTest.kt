package link.yologram.worker.domain.pms.tech.repository

import jakarta.persistence.EntityManager
import link.yologram.worker.domain.pms.tech.entity.TechPostView
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechPostViewRepositoryTest {

    @Autowired
    lateinit var viewRepository: TechPostViewRepository

    @Autowired
    lateinit var entityManager: EntityManager

    private val occurredAt = LocalDateTime.of(2026, 8, 13, 0, 10, 0)

    @BeforeEach
    fun setUp() {
        viewRepository.deleteAll()
        entityManager.flush()
    }

    private fun insert(viewKey: String, postId: Long = 1200, uid: Long? = 12, ip: String? = null) =
        viewRepository.insertIgnore(postId = postId, uid = uid, ip = ip, viewKey = viewKey, occurredAt = occurredAt)

    /** native 삽입 결과를 1차 캐시(stale) 없이 다시 읽는다 */
    private fun findAllFresh(): List<TechPostView> {
        entityManager.clear()
        return viewRepository.findAll()
    }

    @Nested
    inner class insertIgnore {

        @Test
        fun `신규 view_key는 1을 반환하고 행이 생긴다`() {
            val affected = insert("1200:u12:2026-08-13")

            assertEquals(1, affected)
            assertEquals(listOf("1200:u12:2026-08-13"), findAllFresh().map { it.viewKey })
        }

        @Test
        fun `같은 view_key 재삽입은 0을 반환하고 행이 늘지 않는다 (멱등)`() {
            assertEquals(1, insert("1200:u12:2026-08-13"))

            val second = insert("1200:u12:2026-08-13")

            assertEquals(0, second)
            assertEquals(1, findAllFresh().size)
        }

        @Test
        fun `uid가 null이어도 삽입된다 (비로그인)`() {
            val affected = insert("1200:i203.0.113.7:2026-08-13", uid = null, ip = "203.0.113.7")

            assertEquals(1, affected)
            val row = findAllFresh().single()
            assertNull(row.uid)
            assertEquals("203.0.113.7", row.ip)
        }

        @Test
        fun `uid와 ip가 모두 null이어도 삽입된다 (unknown)`() {
            val affected = insert("1200:unknown:2026-08-13", uid = null, ip = null)

            assertEquals(1, affected)
            val row = findAllFresh().single()
            assertNull(row.uid)
            assertNull(row.ip)
        }

        @Test
        fun `IPv6 완전 표기(45자 컬럼)도 저장된다`() {
            val ip = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
            assertEquals(1, insert("1200:i$ip:2026-08-13", uid = null, ip = ip))

            assertEquals(ip, findAllFresh().single().ip)
        }

        @Test
        fun `view_key가 다르면 같은 글이라도 각각 삽입된다`() {
            assertEquals(1, insert("1200:u12:2026-08-13"))
            assertEquals(1, insert("1200:u13:2026-08-13"))
            assertEquals(1, insert("1200:u12:2026-08-14"))

            assertEquals(3, findAllFresh().size)
        }

        @Test
        fun `occurred_at은 이벤트 발생 시각 그대로 기록된다 (viewDate 기준값)`() {
            insert("1200:u12:2026-08-13")

            val row = findAllFresh().single()
            assertEquals(occurredAt, row.occurredAt)
            // created_at은 NOT NULL이고 기본값이 없다 — 삽입이 성공한 것 자체가 쿼리의 NOW(6)로 채워진 증거.
            // 실제 값은 DB 서버 타임존(컨테이너 UTC)에 따라 달라져 시각 비교로 단정하지 않는다
        }
    }

    @Nested
    inner class findExistingViewKeys {

        @Test
        fun `적재된 view_key만 반환한다`() {
            insert("1200:u12:2026-08-13")
            insert("1201:u12:2026-08-13")

            val existing = viewRepository.findExistingViewKeys(
                listOf("1200:u12:2026-08-13", "1201:u12:2026-08-13", "1202:u12:2026-08-13")
            )

            assertEquals(setOf("1200:u12:2026-08-13", "1201:u12:2026-08-13"), existing.toSet())
        }

        @Test
        fun `일치하는 키가 없으면 빈 목록을 반환한다`() {
            assertTrue(viewRepository.findExistingViewKeys(listOf("1200:u99:2026-08-13")).isEmpty())
        }
    }

    @Nested
    inner class deleteOlderThan {

        private fun save(occurredAt: LocalDateTime, viewKey: String) =
            viewRepository.save(
                TechPostView(postId = 1200, uid = 12, viewKey = viewKey, occurredAt = occurredAt)
            )

        @Test
        fun `임계 시각보다 오래된 행만 삭제한다 (경계는 남는다)`() {
            val threshold = LocalDateTime.of(2026, 7, 14, 12, 0, 0)
            save(threshold.minusSeconds(1), "old")     // 삭제 대상
            save(threshold, "boundary")                // 경계 — 남는다 (< 조건)
            save(threshold.plusSeconds(1), "recent")   // 남는다
            entityManager.flush()

            val deleted = viewRepository.deleteOlderThan(threshold, 100)

            assertEquals(1, deleted)
            assertEquals(setOf("boundary", "recent"), findAllFresh().map { it.viewKey }.toSet())
        }

        @Test
        fun `chunkSize를 넘겨 삭제하지 않는다 (청크 반복 전제)`() {
            val threshold = LocalDateTime.of(2026, 7, 14, 12, 0, 0)
            repeat(5) { save(threshold.minusDays(1), "old-$it") }
            entityManager.flush()

            val first = viewRepository.deleteOlderThan(threshold, 2)
            val second = viewRepository.deleteOlderThan(threshold, 2)
            val third = viewRepository.deleteOlderThan(threshold, 2)

            assertEquals(2, first)
            assertEquals(2, second)
            assertEquals(1, third)
            assertTrue(findAllFresh().isEmpty())
        }

        @Test
        fun `삭제 대상이 없으면 0을 반환한다`() {
            save(LocalDateTime.of(2026, 8, 13, 0, 0, 0), "recent")
            entityManager.flush()

            assertEquals(0, viewRepository.deleteOlderThan(LocalDateTime.of(2026, 7, 14, 12, 0, 0), 100))
        }
    }
}
