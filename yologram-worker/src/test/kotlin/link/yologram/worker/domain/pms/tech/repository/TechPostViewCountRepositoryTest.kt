package link.yologram.worker.domain.pms.tech.repository

import jakarta.persistence.EntityManager
import link.yologram.worker.domain.pms.tech.entity.TechPostViewCount
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechPostViewCountRepositoryTest {

    @Autowired
    lateinit var viewCountRepository: TechPostViewCountRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        viewCountRepository.deleteAll()
        entityManager.flush()
    }

    /** native 갱신 결과를 1차 캐시(stale 엔티티) 없이 다시 읽는다 */
    private fun findCount(postId: Long): TechPostViewCount? {
        entityManager.clear()
        return viewCountRepository.findByIdOrNull(postId)
    }

    @Test
    fun `row가 없으면 delta로 생성한다 (upsert)`() {
        viewCountRepository.increase(100L, 1L)

        assertEquals(1L, findCount(100L)?.viewCount)
    }

    @Test
    fun `row가 없고 delta가 여러 건이면 그 값으로 생성한다`() {
        viewCountRepository.increase(100L, 5L)

        assertEquals(5L, findCount(100L)?.viewCount)
    }

    @Test
    fun `row가 있으면 delta만큼 증가시킨다`() {
        viewCountRepository.save(TechPostViewCount(postId = 100L, viewCount = 3L))
        entityManager.flush()

        viewCountRepository.increase(100L, 4L)

        assertEquals(7L, findCount(100L)?.viewCount)
    }

    @Test
    fun `연속 호출 시 delta가 누적된다`() {
        viewCountRepository.increase(100L, 2L)
        viewCountRepository.increase(100L, 3L)

        assertEquals(5L, findCount(100L)?.viewCount)
    }

    @Test
    fun `postId별로 독립적으로 누적된다`() {
        viewCountRepository.increase(100L, 2L)
        viewCountRepository.increase(200L, 7L)

        assertEquals(2L, findCount(100L)?.viewCount)
        assertEquals(7L, findCount(200L)?.viewCount)
    }

    @Test
    fun `갱신하지 않은 postId는 row가 생기지 않는다`() {
        viewCountRepository.increase(100L, 1L)

        assertNull(findCount(999L))
    }
}
