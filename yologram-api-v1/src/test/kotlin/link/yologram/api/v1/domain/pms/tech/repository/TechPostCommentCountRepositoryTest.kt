package link.yologram.api.v1.domain.pms.tech.repository

import jakarta.persistence.EntityManager
import link.yologram.api.v1.domain.pms.tech.entity.TechPostCommentCount
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
class TechPostCommentCountRepositoryTest {

    @Autowired
    lateinit var commentCountRepository: TechPostCommentCountRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        commentCountRepository.deleteAll()
        entityManager.flush()
    }

    /** native 갱신 결과를 1차 캐시(stale 엔티티) 없이 다시 읽는다 */
    private fun findCount(postId: Long): TechPostCommentCount? {
        entityManager.clear()
        return commentCountRepository.findByIdOrNull(postId)
    }

    @Nested
    inner class 증가 {

        @Test
        fun `row가 없으면 comment_count 1로 생성한다 (upsert)`() {
            commentCountRepository.increase(100L)

            assertEquals(1L, findCount(100L)?.commentCount)
        }

        @Test
        fun `row가 있으면 comment_count를 1 증가시킨다`() {
            commentCountRepository.save(TechPostCommentCount(postId = 100L, commentCount = 3L))
            entityManager.flush()

            commentCountRepository.increase(100L)

            assertEquals(4L, findCount(100L)?.commentCount)
        }

        @Test
        fun `연속 호출 시 호출 횟수만큼 누적된다`() {
            commentCountRepository.increase(100L)
            commentCountRepository.increase(100L)
            commentCountRepository.increase(100L)

            assertEquals(3L, findCount(100L)?.commentCount)
        }
    }

    @Nested
    inner class 감소 {

        @Test
        fun `row가 있으면 comment_count를 1 감소시킨다`() {
            commentCountRepository.save(TechPostCommentCount(postId = 100L, commentCount = 2L))
            entityManager.flush()

            commentCountRepository.decrease(100L)

            assertEquals(1L, findCount(100L)?.commentCount)
        }

        @Test
        fun `0에서 감소해도 음수가 되지 않고 0을 유지한다`() {
            commentCountRepository.save(TechPostCommentCount(postId = 100L, commentCount = 0L))
            entityManager.flush()

            commentCountRepository.decrease(100L)

            assertEquals(0L, findCount(100L)?.commentCount)
        }

        @Test
        fun `0이 되어도 row는 삭제하지 않는다`() {
            commentCountRepository.increase(100L)

            commentCountRepository.decrease(100L)

            // count 0 유지 + row 존재 (조회 coalesce가 0 처리하므로 삭제/재생성 churn 없음)
            assertEquals(0L, findCount(100L)?.commentCount)
        }

        @Test
        fun `row가 없으면 아무 일도 없고 row도 생성하지 않는다`() {
            commentCountRepository.decrease(999L)

            assertNull(findCount(999L))
        }
    }
}
