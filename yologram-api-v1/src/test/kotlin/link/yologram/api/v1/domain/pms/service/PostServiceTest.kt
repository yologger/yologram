package link.yologram.api.v1.domain.pms.service

import link.yologram.api.v1.domain.cms.enum.Section
import link.yologram.api.v1.domain.cms.exception.InvalidSectionException
import link.yologram.api.v1.domain.pms.entity.Post
import link.yologram.api.v1.domain.pms.entity.PostCategory
import link.yologram.api.v1.domain.pms.exception.InvalidCategoryException
import link.yologram.api.v1.domain.pms.model.CreatePostRequest
import link.yologram.api.v1.domain.pms.repository.PostCategoryRepository
import link.yologram.api.v1.domain.pms.repository.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class PostServiceTest {

    @Mock
    lateinit var postRepository: PostRepository

    @Mock
    lateinit var postCategoryRepository: PostCategoryRepository

    @Mock
    lateinit var categoryQueryClient: CategoryQueryClient

    @InjectMocks
    lateinit var postService: PostService

    private fun savedPost(id: Long = 1L) =
        Post(id = id, section = Section.TECH, userId = 1L, content = "내용")

    @Nested
    inner class 게시글_작성 {

        @Test
        fun `정상 작성 시 게시글과 카테고리를 저장하고 id를 반환한다`() {
            whenever(categoryQueryClient.allActiveInSection(Section.TECH, setOf(1L, 2L))).thenReturn(true)
            whenever(postRepository.save(any<Post>())).thenReturn(savedPost(10L))

            val result = postService.create("tech", 1L, CreatePostRequest(content = "내용", categoryIds = listOf(1L, 2L)))

            assertEquals(10L, result.id)
            verify(postCategoryRepository, times(2)).save(any<PostCategory>())
        }

        @Test
        fun `카테고리가 비어도 작성된다`() {
            whenever(categoryQueryClient.allActiveInSection(Section.TECH, emptySet())).thenReturn(true)
            whenever(postRepository.save(any<Post>())).thenReturn(savedPost(11L))

            val result = postService.create("tech", 1L, CreatePostRequest(content = "내용", categoryIds = emptyList()))

            assertEquals(11L, result.id)
            verify(postCategoryRepository, never()).save(any<PostCategory>())
        }

        @Test
        fun `카테고리가 해당 section 것이 아니면 InvalidCategoryException을 던진다`() {
            whenever(categoryQueryClient.allActiveInSection(Section.TECH, setOf(99L))).thenReturn(false)

            assertThrows<InvalidCategoryException> {
                postService.create("tech", 1L, CreatePostRequest(content = "내용", categoryIds = listOf(99L)))
            }

            verify(postRepository, never()).save(any<Post>())
        }

        @Test
        fun `유효하지 않은 section이면 InvalidSectionException을 던진다`() {
            assertThrows<InvalidSectionException> {
                postService.create("unknown", 1L, CreatePostRequest(content = "내용"))
            }

            verify(postRepository, never()).save(any<Post>())
        }
    }
}
