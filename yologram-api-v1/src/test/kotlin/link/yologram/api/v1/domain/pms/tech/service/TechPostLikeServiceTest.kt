package link.yologram.api.v1.domain.pms.tech.service

import jakarta.persistence.EntityManager
import link.yologram.api.v1.domain.pms.tech.entity.TechPost
import link.yologram.api.v1.domain.pms.tech.exception.TechPostNotFoundException
import link.yologram.api.v1.domain.pms.tech.repository.TechPostLikeCountRepository
import link.yologram.api.v1.domain.pms.tech.repository.TechPostLikeRepository
import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * 좋아요 통합 테스트 — 이력(tech_post_like)과 카운트(tech_post_like_count)의 정합·멱등을
 * 실제 DB(Testcontainers MySQL)로 검증. INSERT IGNORE·가드 UPDATE는 mock으로 의미가 없어 통합으로 작성.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechPostLikeServiceTest {

    @Autowired
    lateinit var likeService: TechPostLikeService

    @Autowired
    lateinit var postRepository: TechPostRepository

    @Autowired
    lateinit var likeRepository: TechPostLikeRepository

    @Autowired
    lateinit var likeCountRepository: TechPostLikeCountRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        likeRepository.deleteAll()
        likeCountRepository.deleteAll()
        postRepository.deleteAll()
        entityManager.flush()
    }

    private fun savePost(): TechPost = postRepository.save(TechPost(userId = 1L, content = "내용"))

    /** native 갱신 결과를 1차 캐시 없이 다시 읽는다 */
    private fun likeCountOf(postId: Long): Long? {
        entityManager.clear()
        return likeCountRepository.findByIdOrNull(postId)?.likeCount
    }

    @Nested
    inner class 좋아요 {

        @Test
        fun `좋아요 시 이력 삽입 + 카운트 1이 된다`() {
            val post = savePost()

            likeService.like(post.id, 7L)

            assertTrue(likeRepository.existsByPostIdAndUid(post.id, 7L))
            assertEquals(1L, likeCountOf(post.id))
        }

        @Test
        fun `이미 좋아요한 글에 다시 좋아요해도 카운트가 늘지 않는다 (멱등)`() {
            val post = savePost()

            likeService.like(post.id, 7L)
            likeService.like(post.id, 7L)

            // INSERT IGNORE가 중복을 0행 삽입으로 무시 → 카운트 증가 생략, 이력도 1건 유지
            assertEquals(1L, likeCountOf(post.id))
            assertEquals(1, likeRepository.findByUidAndPostIdIn(7L, listOf(post.id)).size)
        }

        @Test
        fun `서로 다른 유저의 좋아요는 각각 집계된다`() {
            val post = savePost()

            likeService.like(post.id, 7L)
            likeService.like(post.id, 8L)

            assertEquals(2L, likeCountOf(post.id))
        }

        @Test
        fun `없는 글에 좋아요하면 TechPostNotFoundException을 던지고 아무것도 남기지 않는다`() {
            assertThrows<TechPostNotFoundException> {
                likeService.like(9999L, 7L)
            }

            assertFalse(likeRepository.existsByPostIdAndUid(9999L, 7L))
            assertNull(likeCountOf(9999L))
        }
    }

    @Nested
    inner class 좋아요_취소 {

        @Test
        fun `취소 시 이력 삭제 + 카운트가 줄어든다`() {
            val post = savePost()
            likeService.like(post.id, 7L)

            likeService.unlike(post.id, 7L)

            assertFalse(likeRepository.existsByPostIdAndUid(post.id, 7L))
            // count 0 유지 + row 존재 (row 삭제 없음 — coalesce가 0 처리)
            assertEquals(0L, likeCountOf(post.id))
        }

        @Test
        fun `안 누른 글을 취소해도 no-op이다 (멱등, 카운트 불변)`() {
            val post = savePost()
            likeService.like(post.id, 8L) // 다른 유저의 좋아요 1개

            likeService.unlike(post.id, 7L) // 7은 안 누른 상태

            // 이력 0행 삭제 → 카운트 감소 생략 (8의 좋아요가 깎이지 않는다)
            assertEquals(1L, likeCountOf(post.id))
        }

        @Test
        fun `취소 후 다시 좋아요하면 카운트가 다시 1이 된다`() {
            val post = savePost()
            likeService.like(post.id, 7L)
            likeService.unlike(post.id, 7L)

            likeService.like(post.id, 7L)

            assertTrue(likeRepository.existsByPostIdAndUid(post.id, 7L))
            assertEquals(1L, likeCountOf(post.id))
        }

        @Test
        fun `없는 글을 취소하면 TechPostNotFoundException을 던진다`() {
            assertThrows<TechPostNotFoundException> {
                likeService.unlike(9999L, 7L)
            }
        }
    }
}
