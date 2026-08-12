package link.yologram.api.v1.domain.pms.tech.repository

import jakarta.persistence.EntityManager
import link.yologram.api.v1.domain.pms.tech.entity.TechPostLikeCount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechPostLikeCountRepositoryTest {

    @Autowired
    lateinit var likeCountRepository: TechPostLikeCountRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        likeCountRepository.deleteAll()
        entityManager.flush()
    }

    /** native 갱신 결과를 1차 캐시(stale 엔티티) 없이 다시 읽는다 */
    private fun findCount(postId: Long): TechPostLikeCount? {
        entityManager.clear()
        return likeCountRepository.findByIdOrNull(postId)
    }

    @Nested
    inner class 증가 {

        @Test
        fun `row가 없으면 like_count 1로 생성한다 (upsert)`() {
            likeCountRepository.increase(100L)

            assertEquals(1L, findCount(100L)?.likeCount)
        }

        @Test
        fun `row가 있으면 like_count를 1 증가시킨다`() {
            likeCountRepository.save(TechPostLikeCount(postId = 100L, likeCount = 3L))
            entityManager.flush()

            likeCountRepository.increase(100L)

            assertEquals(4L, findCount(100L)?.likeCount)
        }

        @Test
        fun `연속 호출 시 호출 횟수만큼 누적된다`() {
            likeCountRepository.increase(100L)
            likeCountRepository.increase(100L)
            likeCountRepository.increase(100L)

            assertEquals(3L, findCount(100L)?.likeCount)
        }
    }

    @Nested
    inner class 감소 {

        @Test
        fun `row가 있으면 like_count를 1 감소시킨다`() {
            likeCountRepository.save(TechPostLikeCount(postId = 100L, likeCount = 2L))
            entityManager.flush()

            likeCountRepository.decrease(100L)

            assertEquals(1L, findCount(100L)?.likeCount)
        }

        @Test
        fun `0에서 감소해도 음수가 되지 않고 0을 유지한다`() {
            likeCountRepository.save(TechPostLikeCount(postId = 100L, likeCount = 0L))
            entityManager.flush()

            likeCountRepository.decrease(100L)

            assertEquals(0L, findCount(100L)?.likeCount)
        }

        @Test
        fun `0이 되어도 row는 삭제하지 않는다`() {
            likeCountRepository.increase(100L)

            likeCountRepository.decrease(100L)

            // count 0 유지 + row 존재 (조회 coalesce가 0 처리하므로 삭제/재생성 churn 없음)
            assertEquals(0L, findCount(100L)?.likeCount)
        }

        @Test
        fun `row가 없으면 아무 일도 없고 row도 생성하지 않는다`() {
            likeCountRepository.decrease(999L)

            assertNull(findCount(999L))
        }
    }
}
