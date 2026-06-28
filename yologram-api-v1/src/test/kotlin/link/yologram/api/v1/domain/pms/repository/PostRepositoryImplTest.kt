package link.yologram.api.v1.domain.pms.repository

import jakarta.persistence.EntityManager
import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.pms.entity.Post
import link.yologram.api.v1.domain.pms.entity.PostCategoryMapping
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
class PostRepositoryImplTest {

    @Autowired
    lateinit var postRepository: PostRepository

    @Autowired
    lateinit var postCategoryMappingRepository: PostCategoryMappingRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        postCategoryMappingRepository.deleteAll()
        postRepository.deleteAll()
        entityManager.flush()
    }

    private fun savePost(section: Section, userId: Long = 1L): Post =
        postRepository.save(Post(section = section, userId = userId, content = "내용"))

    @Nested
    inner class 섹션_필터 {

        @Test
        fun `요청한 section의 글만 반환한다`() {
            savePost(Section.TECH)
            savePost(Section.INVEST)
            savePost(Section.TECH)

            val result = postRepository.findPostsBySection(Section.TECH, null, null, 10)

            assertEquals(2, result.size)
            assertTrue(result.all { it.section == Section.TECH })
        }
    }

    @Nested
    inner class 정렬 {

        @Test
        fun `id 내림차순(최신순)으로 정렬한다`() {
            val a = savePost(Section.TECH)
            val b = savePost(Section.TECH)
            val c = savePost(Section.TECH)

            val result = postRepository.findPostsBySection(Section.TECH, null, null, 10)

            assertEquals(listOf(c.id, b.id, a.id), result.map { it.id })
        }
    }

    @Nested
    inner class 커서_페이지네이션 {

        @Test
        fun `커서(id) 이후 더 과거 글만 반환하며 누락·중복이 없다`() {
            val p1 = savePost(Section.TECH)
            val p2 = savePost(Section.TECH)
            val p3 = savePost(Section.TECH)

            // id desc → p3, p2, p1
            val firstPage = postRepository.findPostsBySection(Section.TECH, null, null, 2)
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.id })

            val cursorId = firstPage.last().id
            val secondPage = postRepository.findPostsBySection(Section.TECH, null, cursorId, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.id })
        }
    }

    @Nested
    inner class 카테고리_필터 {

        @Test
        fun `categoryId가 매핑된 글만 반환한다`() {
            val p1 = savePost(Section.TECH)
            val p2 = savePost(Section.TECH)
            postCategoryMappingRepository.save(PostCategoryMapping(postId = p1.id, categoryId = 10L))
            postCategoryMappingRepository.save(PostCategoryMapping(postId = p2.id, categoryId = 20L))
            entityManager.flush()

            val result = postRepository.findPostsBySection(Section.TECH, 10L, null, 10)

            assertEquals(listOf(p1.id), result.map { it.id })
        }
    }

    @Nested
    inner class 개수_제한 {

        @Test
        fun `limit만큼만 반환한다`() {
            repeat(5) { savePost(Section.TECH) }

            val result = postRepository.findPostsBySection(Section.TECH, null, null, 3)

            assertEquals(3, result.size)
        }
    }

    @Nested
    inner class 내_글_목록 {

        @Test
        fun `해당 유저의 글만 반환한다`() {
            savePost(Section.TECH, userId = 1L)
            savePost(Section.TECH, userId = 2L)
            savePost(Section.TECH, userId = 1L)

            val result = postRepository.findMyPosts(1L, null, 0L, 10)

            assertEquals(2, result.size)
            assertTrue(result.all { it.userId == 1L })
        }

        @Test
        fun `section이 지정되면 해당 section 내 글만 반환한다`() {
            savePost(Section.TECH, userId = 1L)
            savePost(Section.INVEST, userId = 1L)

            val result = postRepository.findMyPosts(1L, Section.TECH, 0L, 10)

            assertEquals(1, result.size)
            assertTrue(result.all { it.section == Section.TECH })
        }

        @Test
        fun `id 내림차순(최신순)으로 정렬한다`() {
            val a = savePost(Section.TECH, userId = 1L)
            val b = savePost(Section.TECH, userId = 1L)
            val c = savePost(Section.TECH, userId = 1L)

            val result = postRepository.findMyPosts(1L, null, 0L, 10)

            assertEquals(listOf(c.id, b.id, a.id), result.map { it.id })
        }

        @Test
        fun `offset과 limit으로 페이지를 건너뛴다`() {
            val p1 = savePost(Section.TECH, userId = 1L)
            val p2 = savePost(Section.TECH, userId = 1L)
            val p3 = savePost(Section.TECH, userId = 1L)

            // id desc → p3, p2, p1
            val firstPage = postRepository.findMyPosts(1L, null, 0L, 2)
            assertEquals(listOf(p3.id, p2.id), firstPage.map { it.id })

            val secondPage = postRepository.findMyPosts(1L, null, 2L, 2)
            assertEquals(listOf(p1.id), secondPage.map { it.id })
        }

        @Test
        fun `countMyPosts는 유저의 전체·section별 글 수를 센다`() {
            savePost(Section.TECH, userId = 1L)
            savePost(Section.INVEST, userId = 1L)
            savePost(Section.TECH, userId = 2L)

            assertEquals(2L, postRepository.countMyPosts(1L, null))
            assertEquals(1L, postRepository.countMyPosts(1L, Section.TECH))
        }

        @Test
        fun `findMyPosts는 해당 유저 글만 cursor 이후로 반환한다`() {
            val p1 = savePost(Section.TECH, userId = 1L)
            savePost(Section.TECH, userId = 2L)
            val p3 = savePost(Section.TECH, userId = 1L)

            // 내 글 id desc → p3, p1 (다른 유저 글 제외)
            val firstPage = postRepository.findMyPosts(1L, null, null, 1)
            assertEquals(listOf(p3.id), firstPage.map { it.id })
            assertTrue(firstPage.all { it.userId == 1L })

            // cursor(p3.id) 이후 → p1. cursorId는 Long?로 줘야 cursor 오버로드가 선택됨(Long이면 offset 오버로드로 감)
            val cursorId: Long? = p3.id
            val secondPage = postRepository.findMyPosts(1L, null, cursorId, 1)
            assertEquals(listOf(p1.id), secondPage.map { it.id })
        }
    }
}
