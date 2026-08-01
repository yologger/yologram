package link.yologram.api.v1.domain.pms.tech.repository

import jakarta.persistence.EntityManager
import link.yologram.api.v1.domain.pms.tech.entity.TechPost
import link.yologram.api.v1.domain.pms.tech.entity.TechPostCategoryMapping
import org.junit.jupiter.api.Assertions.assertEquals
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
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        postCategoryMappingRepository.deleteAll()
        postRepository.deleteAll()
        entityManager.flush()
    }

    private fun savePost(userId: Long = 1L): TechPost =
        postRepository.save(TechPost(userId = userId, content = "내용"))

    @Nested
    inner class 정렬 {

        @Test
        fun `id 내림차순(최신순)으로 정렬한다`() {
            val a = savePost()
            val b = savePost()
            val c = savePost()

            val result = postRepository.findPosts(null, null, 10)

            assertEquals(listOf(c.id, b.id, a.id), result.map { it.id })
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
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.id })

            // cursorId는 Long?로 줘야 cursor 오버로드가 선택됨(Long이면 offset 오버로드로 감)
            val cursorId: Long? = firstPage.last().id
            val secondPage = postRepository.findPosts(null, cursorId, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.id })
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

            assertEquals(listOf(p1.id), result.map { it.id })
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
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.id })

            val secondPage = postRepository.findPosts(null, 2L, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.id })
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
            assertTrue(result.all { it.userId == 1L })
        }

        @Test
        fun `id 내림차순(최신순)으로 정렬한다`() {
            val a = savePost(userId = 1L)
            val b = savePost(userId = 1L)
            val c = savePost(userId = 1L)

            val result = postRepository.findMyPosts(1L, 0L, 10)

            assertEquals(listOf(c.id, b.id, a.id), result.map { it.id })
        }

        @Test
        fun `offset과 limit으로 페이지를 건너뛴다`() {
            val p1 = savePost(userId = 1L)
            val p2 = savePost(userId = 1L)
            val p3 = savePost(userId = 1L)

            // id desc → p3, p2, p1
            val firstPage = postRepository.findMyPosts(1L, 0L, 2)
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.id })

            val secondPage = postRepository.findMyPosts(1L, 2L, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.id })
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
            assertEquals(listOf(p3.id), firstPage.map { it.id })
            assertTrue(firstPage.all { it.userId == 1L })

            // cursor(p3.id) 이후 → p1. cursorId는 Long?로 줘야 cursor 오버로드가 선택됨(Long이면 offset 오버로드로 감)
            val cursorId: Long? = p3.id
            val secondPage = postRepository.findMyPosts(1L, cursorId, 1)
            assertEquals(listOf(p1.id), secondPage.map { it.id })
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
