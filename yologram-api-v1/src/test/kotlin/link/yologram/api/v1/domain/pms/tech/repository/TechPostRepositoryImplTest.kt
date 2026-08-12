package link.yologram.api.v1.domain.pms.tech.repository

import jakarta.persistence.EntityManager
import link.yologram.api.v1.domain.pms.tech.entity.TechPost
import link.yologram.api.v1.domain.pms.tech.entity.TechPostCategoryMapping
import link.yologram.api.v1.domain.pms.tech.entity.TechPostCommentCount
import link.yologram.api.v1.domain.pms.tech.entity.TechPostLikeCount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechPostRepositoryImplTest {

    @Autowired
    lateinit var postRepository: TechPostRepository

    @Autowired
    lateinit var postCategoryMappingRepository: TechPostCategoryMappingRepository

    @Autowired
    lateinit var postCommentCountRepository: TechPostCommentCountRepository

    @Autowired
    lateinit var postLikeCountRepository: TechPostLikeCountRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        postCategoryMappingRepository.deleteAll()
        postCommentCountRepository.deleteAll()
        postLikeCountRepository.deleteAll()
        postRepository.deleteAll()
        entityManager.flush()
    }

    private fun savePost(userId: Long = 1L): TechPost =
        postRepository.save(TechPost(userId = userId, content = "내용"))

    private fun saveCommentCount(postId: Long, count: Long) {
        postCommentCountRepository.save(TechPostCommentCount(postId = postId, commentCount = count))
        entityManager.flush()
    }

    private fun saveLikeCount(postId: Long, count: Long) {
        postLikeCountRepository.save(TechPostLikeCount(postId = postId, likeCount = count))
        entityManager.flush()
    }

    @Nested
    inner class 정렬 {

        @Test
        fun `id 내림차순(최신순)으로 정렬한다`() {
            val a = savePost()
            val b = savePost()
            val c = savePost()

            val result = postRepository.findPosts(null, null, 10)

            assertEquals(listOf(c.id, b.id, a.id), result.map { it.post.id })
        }
    }

    @Nested
    inner class 커서_페이지네이션 {

        @Test
        fun `커서(id) 이후 더 과거 글만 반환하며 누락·중복이 없다`() {
            val p1 = savePost()
            val p2 = savePost()
            val p3 = savePost()

            // id desc → p3, p2, p1
            val firstPage = postRepository.findPosts(null, null, 2)
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.post.id })

            // cursorId는 Long?로 줘야 cursor 오버로드가 선택됨(Long이면 offset 오버로드로 감)
            val cursorId: Long? = firstPage.last().post.id
            val secondPage = postRepository.findPosts(null, cursorId, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.post.id })
        }
    }

    @Nested
    inner class 카테고리_필터 {

        @Test
        fun `categoryId가 매핑된 글만 반환한다`() {
            val p1 = savePost()
            val p2 = savePost()
            postCategoryMappingRepository.save(TechPostCategoryMapping(postId = p1.id, categoryId = 10L))
            postCategoryMappingRepository.save(TechPostCategoryMapping(postId = p2.id, categoryId = 20L))
            entityManager.flush()

            val result = postRepository.findPosts(10L, null, 10)

            assertEquals(listOf(p1.id), result.map { it.post.id })
        }
    }

    @Nested
    inner class 개수_제한 {

        @Test
        fun `limit만큼만 반환한다`() {
            repeat(5) { savePost() }

            val result = postRepository.findPosts(null, null, 3)

            assertEquals(3, result.size)
        }
    }

    @Nested
    inner class 피드_offset {

        @Test
        fun `offset과 limit으로 페이지를 건너뛴다`() {
            val p1 = savePost()
            val p2 = savePost()
            val p3 = savePost()

            // id desc → p3, p2, p1. 2번째 인자가 Long(offset)이라 offset 오버로드가 선택됨
            val firstPage = postRepository.findPosts(null, 0L, 2)
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.post.id })

            val secondPage = postRepository.findPosts(null, 2L, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.post.id })
        }

        @Test
        fun `countPosts는 전체·카테고리별 글 수를 센다`() {
            val p1 = savePost()
            savePost()
            postCategoryMappingRepository.save(TechPostCategoryMapping(postId = p1.id, categoryId = 10L))
            entityManager.flush()

            assertEquals(2L, postRepository.countPosts(null))
            assertEquals(1L, postRepository.countPosts(10L))
        }
    }

    @Nested
    inner class 내_글_목록 {

        @Test
        fun `해당 유저의 글만 반환한다`() {
            savePost(userId = 1L)
            savePost(userId = 2L)
            savePost(userId = 1L)

            val result = postRepository.findMyPosts(1L, 0L, 10)

            assertEquals(2, result.size)
            assertTrue(result.all { it.post.userId == 1L })
        }

        @Test
        fun `id 내림차순(최신순)으로 정렬한다`() {
            val a = savePost(userId = 1L)
            val b = savePost(userId = 1L)
            val c = savePost(userId = 1L)

            val result = postRepository.findMyPosts(1L, 0L, 10)

            assertEquals(listOf(c.id, b.id, a.id), result.map { it.post.id })
        }

        @Test
        fun `offset과 limit으로 페이지를 건너뛴다`() {
            val p1 = savePost(userId = 1L)
            val p2 = savePost(userId = 1L)
            val p3 = savePost(userId = 1L)

            // id desc → p3, p2, p1
            val firstPage = postRepository.findMyPosts(1L, 0L, 2)
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.post.id })

            val secondPage = postRepository.findMyPosts(1L, 2L, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.post.id })
        }

        @Test
        fun `countMyPosts는 유저의 전체 글 수를 센다`() {
            savePost(userId = 1L)
            savePost(userId = 1L)
            savePost(userId = 2L)

            assertEquals(2L, postRepository.countMyPosts(1L))
            assertEquals(1L, postRepository.countMyPosts(2L))
        }

        @Test
        fun `findMyPosts는 해당 유저 글만 cursor 이후로 반환한다`() {
            val p1 = savePost(userId = 1L)
            savePost(userId = 2L)
            val p3 = savePost(userId = 1L)

            // 내 글 id desc → p3, p1 (다른 유저 글 제외)
            val firstPage = postRepository.findMyPosts(1L, null, 1)
            assertEquals(listOf(p3.id), firstPage.map { it.post.id })
            assertTrue(firstPage.all { it.post.userId == 1L })

            // cursor(p3.id) 이후 → p1. cursorId는 Long?로 줘야 cursor 오버로드가 선택됨(Long이면 offset 오버로드로 감)
            val cursorId: Long? = p3.id
            val secondPage = postRepository.findMyPosts(1L, cursorId, 1)
            assertEquals(listOf(p1.id), secondPage.map { it.post.id })
        }
    }

    @Nested
    inner class 댓글_수_조인 {

        @Test
        fun `목록에서 count row가 있는 글은 실값, 없는 글은 0으로 반환한다`() {
            val withComments = savePost()
            val withoutComments = savePost()
            saveCommentCount(withComments.id, 3L)

            // id desc → withoutComments(0), withComments(3). leftJoin+coalesce라 row 없는 글도 목록에 남는다
            val result = postRepository.findPosts(null, null, 10)

            assertEquals(listOf(withoutComments.id, withComments.id), result.map { it.post.id })
            assertEquals(listOf(0L, 3L), result.map { it.commentCount })
        }

        @Test
        fun `상세에서 count row가 있는 글은 실값을 반환한다`() {
            val post = savePost()
            saveCommentCount(post.id, 5L)

            val result = postRepository.findPostWithCounts(post.id)

            assertEquals(post.id, result?.post?.id)
            assertEquals(5L, result?.commentCount)
        }

        @Test
        fun `상세에서 count row가 없는 글은 0을 반환한다 (coalesce)`() {
            val post = savePost()

            val result = postRepository.findPostWithCounts(post.id)

            assertEquals(0L, result?.commentCount)
        }

        @Test
        fun `상세에서 없는 글이면 null을 반환한다`() {
            assertNull(postRepository.findPostWithCounts(9999L))
        }

        @Test
        fun `내 글 목록에도 댓글 수가 실린다`() {
            val mine = savePost(userId = 1L)
            saveCommentCount(mine.id, 2L)

            val result = postRepository.findMyPosts(1L, 0L, 10)

            assertEquals(listOf(2L), result.map { it.commentCount })
        }

        @Test
        fun `count 조인 후에도 커서 페이지네이션이 동일하게 동작한다`() {
            val p1 = savePost()
            val p2 = savePost()
            val p3 = savePost()
            // 1:1 조인이라 row가 불어나지 않아 정렬·커서·limit 불변 (일부 글만 count row 보유)
            saveCommentCount(p1.id, 1L)
            saveCommentCount(p3.id, 4L)

            val firstPage = postRepository.findPosts(null, null, 2)
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.post.id })
            assertEquals(listOf(4L, 0L), firstPage.map { it.commentCount })

            val cursorId: Long? = firstPage.last().post.id
            val secondPage = postRepository.findPosts(null, cursorId, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.post.id })
            assertEquals(listOf(1L), secondPage.map { it.commentCount })
        }
    }

    @Nested
    inner class 좋아요_수_조인 {

        @Test
        fun `목록에서 like count row가 있는 글은 실값, 없는 글은 0으로 반환한다`() {
            val withLikes = savePost()
            val withoutLikes = savePost()
            saveLikeCount(withLikes.id, 3L)

            // id desc → withoutLikes(0), withLikes(3). leftJoin+coalesce라 row 없는 글도 목록에 남는다
            val result = postRepository.findPosts(null, null, 10)

            assertEquals(listOf(withoutLikes.id, withLikes.id), result.map { it.post.id })
            assertEquals(listOf(0L, 3L), result.map { it.likeCount })
        }

        @Test
        fun `상세에서 like count row가 있는 글은 실값, 없는 글은 0을 반환한다 (coalesce)`() {
            val withLikes = savePost()
            val withoutLikes = savePost()
            saveLikeCount(withLikes.id, 5L)

            assertEquals(5L, postRepository.findPostWithCounts(withLikes.id)?.likeCount)
            assertEquals(0L, postRepository.findPostWithCounts(withoutLikes.id)?.likeCount)
        }

        @Test
        fun `댓글 수·좋아요 수 조인이 서로 독립적으로 채워진다`() {
            // 카운트 테이블 2개를 동시에 leftJoin — 한쪽만 row가 있어도 각자 coalesce로 채워진다
            val post = savePost()
            saveCommentCount(post.id, 2L)

            val result = postRepository.findPostWithCounts(post.id)

            assertEquals(2L, result?.commentCount)
            assertEquals(0L, result?.likeCount)
        }

        @Test
        fun `내 글 목록에도 좋아요 수가 실린다`() {
            val mine = savePost(userId = 1L)
            saveLikeCount(mine.id, 4L)

            val result = postRepository.findMyPosts(1L, 0L, 10)

            assertEquals(listOf(4L), result.map { it.likeCount })
        }
    }

    @Nested
    inner class 카테고리_매핑_교체 {

        @Test
        fun `deleteByPostId 후 동일 categoryId를 재삽입해도 충돌이 없다`() {
            // 게시글 수정 시 카테고리 매핑 교체 시나리오 — derived deleteBy면 flush 순서상 insert가
            // 먼저 나가 uk_tech_post_category_mapping(post_id, category_id) 충돌. @Modifying 벌크 delete로 즉시 삭제돼야 안전
            val post = savePost()
            postCategoryMappingRepository.save(TechPostCategoryMapping(postId = post.id, categoryId = 2L))
            entityManager.flush()
            entityManager.clear()

            postCategoryMappingRepository.deleteByPostId(post.id)
            postCategoryMappingRepository.save(TechPostCategoryMapping(postId = post.id, categoryId = 2L))
            entityManager.flush()

            assertEquals(listOf(2L), postCategoryMappingRepository.findByPostId(post.id).map { it.categoryId })
        }
    }
}
