package link.yologram.api.v1.domain.pms.repository

import jakarta.persistence.EntityManager
import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.pms.entity.Post
import link.yologram.api.v1.domain.pms.entity.PostCategory
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
    lateinit var postCategoryRepository: PostCategoryRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        postCategoryRepository.deleteAll()
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
            postCategoryRepository.save(PostCategory(postId = p1.id, categoryId = 10L))
            postCategoryRepository.save(PostCategory(postId = p2.id, categoryId = 20L))
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
}
