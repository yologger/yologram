package link.yologram.api.v1.domain.pms.service

import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.cms.exception.InvalidSectionException
import link.yologram.api.v1.domain.pms.entity.Post
import link.yologram.api.v1.domain.pms.entity.PostCategoryMapping
import link.yologram.api.v1.domain.pms.exception.InvalidPostCategoryException
import link.yologram.api.v1.domain.pms.exception.PostForbiddenException
import link.yologram.api.v1.domain.pms.exception.PostNotFoundException
import link.yologram.api.v1.domain.pms.model.CreatePostRequest
import link.yologram.api.v1.domain.pms.model.UpdatePostRequest
import link.yologram.api.v1.domain.pms.model.PostCursor
import link.yologram.api.v1.domain.pms.repository.PostCategoryMappingRepository
import link.yologram.api.v1.domain.pms.repository.PostRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import java.util.Optional
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class PostServiceTest {

    @Mock
    lateinit var postRepository: PostRepository

    @Mock
    lateinit var postCategoryMappingRepository: PostCategoryMappingRepository

    @Mock
    lateinit var categoryQueryClient: PostCategoryQueryClient

    @Mock
    lateinit var userQueryClient: UserQueryClient

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
            verify(postCategoryMappingRepository, times(2)).save(any<PostCategoryMapping>())
        }

        @Test
        fun `카테고리가 해당 section 것이 아니면 InvalidPostCategoryException을 던진다`() {
            whenever(categoryQueryClient.allActiveInSection(Section.TECH, setOf(99L))).thenReturn(false)

            assertThrows<InvalidPostCategoryException> {
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

    @Nested
    inner class 게시글_수정 {

        @Test
        fun `본인 글이면 제목·내용 수정 후 카테고리를 교체한다`() {
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(Post(id = 1L, section = Section.TECH, userId = 1L, content = "원본")))
            whenever(categoryQueryClient.allActiveInSection(Section.TECH, setOf(2L, 3L))).thenReturn(true)

            postService.update("tech", 1L, 1L, UpdatePostRequest(title = "새 제목", content = "새 내용", categoryIds = listOf(2L, 3L)))

            verify(postCategoryMappingRepository).deleteByPostId(1L)
            verify(postCategoryMappingRepository, times(2)).save(any<PostCategoryMapping>())
        }

        @Test
        fun `존재하지 않는 글이면 PostNotFoundException을 던진다`() {
            whenever(postRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<PostNotFoundException> {
                postService.update("tech", 99L, 1L, UpdatePostRequest(content = "내용", categoryIds = listOf(1L)))
            }
        }

        @Test
        fun `다른 section의 글이면 PostNotFoundException을 던진다`() {
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(Post(id = 1L, section = Section.INVEST, userId = 1L, content = "내용")))

            assertThrows<PostNotFoundException> {
                postService.update("tech", 1L, 1L, UpdatePostRequest(content = "내용", categoryIds = listOf(1L)))
            }
        }

        @Test
        fun `본인 글이 아니면 PostForbiddenException을 던진다`() {
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(Post(id = 1L, section = Section.TECH, userId = 99L, content = "내용")))

            assertThrows<PostForbiddenException> {
                postService.update("tech", 1L, 1L, UpdatePostRequest(content = "내용", categoryIds = listOf(1L)))
            }

            verify(postCategoryMappingRepository, never()).deleteByPostId(any())
        }

        @Test
        fun `카테고리가 해당 section 것이 아니면 InvalidPostCategoryException을 던진다`() {
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(Post(id = 1L, section = Section.TECH, userId = 1L, content = "내용")))
            whenever(categoryQueryClient.allActiveInSection(Section.TECH, setOf(99L))).thenReturn(false)

            assertThrows<InvalidPostCategoryException> {
                postService.update("tech", 1L, 1L, UpdatePostRequest(content = "내용", categoryIds = listOf(99L)))
            }

            verify(postCategoryMappingRepository, never()).deleteByPostId(any())
        }
    }

    @Nested
    inner class 게시글_상세_조회 {

        @Test
        fun `게시글과 카테고리·작성자 닉네임을 반환한다`() {
            val post = Post(id = 1L, section = Section.TECH, userId = 12L, title = "제목", content = "내용", likeCount = 3, commentCount = 2)
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))
            whenever(postCategoryMappingRepository.findByPostId(1L)).thenReturn(
                listOf(PostCategoryMapping(id = 1L, postId = 1L, categoryId = 1L), PostCategoryMapping(id = 2L, postId = 1L, categoryId = 2L)),
            )
            whenever(userQueryClient.findNickname(12L)).thenReturn("tester")

            val result = postService.getPost("tech", 1L)

            assertEquals(1L, result.id)
            assertEquals(12L, result.author.uid)
            assertEquals("tester", result.author.nickname)
            assertEquals(listOf(1L, 2L), result.categoryIds)
            assertEquals("내용", result.content)
            assertEquals(2, result.commentCount)
        }

        @Test
        fun `존재하지 않는 게시글이면 PostNotFoundException을 던진다`() {
            whenever(postRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<PostNotFoundException> {
                postService.getPost("tech", 99L)
            }
        }

        @Test
        fun `id가 해당 section 글이 아니면 PostNotFoundException을 던진다`() {
            val post = Post(id = 1L, section = Section.INVEST, userId = 12L, content = "내용")
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

            assertThrows<PostNotFoundException> {
                postService.getPost("tech", 1L)
            }
        }
    }

    @Nested
    inner class 게시글_목록_조회 {

        private fun post(id: Long, userId: Long = id) =
            Post(id = id, section = Section.TECH, userId = userId, content = "내용$id")

        @Test
        fun `결과가 있으면 마지막 글 id를 nextCursor로 반환한다`() {
            val fetched = listOf(post(3), post(2))
            whenever(postRepository.findPostsBySection(eqSection(), anyOrNull(), isNull<Long>(), eq(2))).thenReturn(fetched)
            whenever(userQueryClient.findNicknames(any())).thenReturn(mapOf(3L to "u3", 2L to "u2"))
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(
                listOf(PostCategoryMapping(id = 1L, postId = 3L, categoryId = 10L)),
            )

            val result = postService.getPostsByCursor("tech", null, null, 2)

            assertEquals(2, result.data.size)
            assertEquals(listOf(3L, 2L), result.data.map { it.id })
            assertEquals(listOf(10L), result.data[0].categoryIds)
            assertEquals("u3", result.data[0].author.nickname)
            // 마지막 글 id(2)를 인코딩한 값
            assertEquals(PostCursor.encode(2L), result.nextCursor)
        }

        @Test
        fun `결과가 없으면 빈 목록과 null nextCursor를 반환한다`() {
            whenever(postRepository.findPostsBySection(eqSection(), anyOrNull(), isNull<Long>(), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            val result = postService.getPostsByCursor("tech", null, null, 20)

            assertEquals(0, result.data.size)
            assertNull(result.nextCursor)
        }

        @Test
        fun `cursor가 주어지면 디코딩한 id로 조회한다`() {
            whenever(postRepository.findPostsBySection(eqSection(), anyOrNull(), eq<Long?>(5L), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getPostsByCursor("tech", null, PostCursor.encode(5L), 20)

            verify(postRepository).findPostsBySection(eqSection(), anyOrNull(), eq<Long?>(5L), eq(20))
        }

        @Test
        fun `size가 최대치를 넘으면 50으로 제한된다`() {
            whenever(postRepository.findPostsBySection(eqSection(), anyOrNull(), isNull<Long>(), eq(50))).thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getPostsByCursor("tech", null, null, 100)

            verify(postRepository).findPostsBySection(eqSection(), anyOrNull(), isNull<Long>(), eq(50))
        }

        @Test
        fun `유효하지 않은 section이면 InvalidSectionException을 던진다`() {
            assertThrows<InvalidSectionException> {
                postService.getPostsByCursor("unknown", null, null, 20)
            }

            verify(postRepository, never()).findPostsBySection(any(), anyOrNull(), isNull<Long>(), any())
        }

        private fun eqSection() = eq(Section.TECH)
    }

    // 섹션 피드 offset은 엔드포인트 비활성(PostResource에서 주석)이라 학습용으로 테스트도 주석 처리
    /*
    @Nested
    inner class 섹션_피드_offset_학습용 {

        private fun post(id: Long) = Post(id = id, section = Section.TECH, userId = id, content = "내용$id")

        @Test
        fun `섹션 피드 offset 목록과 페이지 메타를 반환한다`() {
            whenever(postRepository.countPostsBySection(eq(Section.TECH), anyOrNull())).thenReturn(3L)
            whenever(postRepository.findPostsBySection(eq(Section.TECH), anyOrNull(), eq(0L), eq(20)))
                .thenReturn(listOf(post(3), post(2), post(1)))
            whenever(userQueryClient.findNicknames(any())).thenReturn(mapOf(3L to "u3", 2L to "u2", 1L to "u1"))
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            val result = postService.getPostsByOffset("tech", null, 0, 20)

            assertEquals(listOf(3L, 2L, 1L), result.data.map { it.id })
            assertEquals(3L, result.totalCount)
            assertEquals(1L, result.totalPages)
            assertEquals(true, result.first)
            assertEquals(true, result.last)
        }

        @Test
        fun `유효하지 않은 section이면 InvalidSectionException을 던진다`() {
            assertThrows<InvalidSectionException> {
                postService.getPostsByOffset("unknown", null, 0, 20)
            }
        }
    }
    */

    @Nested
    inner class 내_글_목록_cursor {

        private fun post(id: Long) =
            Post(id = id, section = Section.TECH, userId = 1L, content = "내용$id")

        @Test
        fun `결과가 있으면 마지막 글 id를 nextCursor로 반환한다`() {
            whenever(postRepository.findMyPosts(eq(1L), anyOrNull(), isNull<Long>(), eq(2)))
                .thenReturn(listOf(post(3), post(2)))
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(
                listOf(PostCategoryMapping(id = 1L, postId = 3L, categoryId = 10L)),
            )

            val result = postService.getMyPostsByCursor(1L, null, null, 2)

            assertEquals(listOf(3L, 2L), result.data.map { it.id })
            assertEquals("me", result.data[0].author.nickname)
            assertEquals(listOf(10L), result.data[0].categoryIds)
            assertEquals(PostCursor.encode(2L), result.nextCursor)
        }

        @Test
        fun `결과가 없으면 빈 목록과 null nextCursor를 반환한다`() {
            whenever(postRepository.findMyPosts(eq(1L), anyOrNull(), isNull<Long>(), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            val result = postService.getMyPostsByCursor(1L, null, null, 20)

            assertEquals(0, result.data.size)
            assertNull(result.nextCursor)
        }

        @Test
        fun `cursor가 주어지면 디코딩한 id로 조회한다`() {
            whenever(postRepository.findMyPosts(eq(1L), anyOrNull(), eq<Long?>(5L), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getMyPostsByCursor(1L, null, PostCursor.encode(5L), 20)

            verify(postRepository).findMyPosts(eq(1L), anyOrNull(), eq<Long?>(5L), eq(20))
        }

        @Test
        fun `section이 지정되면 해당 section으로 조회한다`() {
            whenever(postRepository.findMyPosts(eq(1L), eq(Section.INVEST), isNull<Long>(), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getMyPostsByCursor(1L, "invest", null, 20)

            verify(postRepository).findMyPosts(eq(1L), eq(Section.INVEST), isNull<Long>(), eq(20))
        }

        @Test
        fun `유효하지 않은 section이면 InvalidSectionException을 던진다`() {
            assertThrows<InvalidSectionException> {
                postService.getMyPostsByCursor(1L, "unknown", null, 20)
            }

            verify(postRepository, never()).findMyPosts(any(), anyOrNull(), isNull<Long>(), any())
        }
    }

    // offset 엔드포인트는 현재 비활성(PostResource에서 주석)이라 학습용으로 테스트도 주석 처리
    /*
    @Nested
    inner class 내_글_목록_offset_학습용 {

        private fun post(id: Long) =
            Post(id = id, section = Section.TECH, userId = 1L, content = "내용$id")

        @Test
        fun `내 글 목록과 페이지 메타를 반환한다`() {
            whenever(postRepository.countMyPosts(eq(1L), anyOrNull())).thenReturn(3L)
            whenever(postRepository.findMyPosts(eq(1L), anyOrNull(), eq(0L), eq(20)))
                .thenReturn(listOf(post(3), post(2), post(1)))
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(
                listOf(PostCategoryMapping(id = 1L, postId = 3L, categoryId = 10L)),
            )

            val result = postService.getMyPostsByOffset(1L, null, 0, 20)

            assertEquals(listOf(3L, 2L, 1L), result.data.map { it.id })
            assertEquals("me", result.data[0].author.nickname)
            assertEquals(listOf(10L), result.data[0].categoryIds)
            assertEquals(3L, result.totalCount)
            assertEquals(1L, result.totalPages)
            assertEquals(0L, result.page)
            assertEquals(true, result.first)
            assertEquals(true, result.last)
        }

        @Test
        fun `결과가 없으면 빈 목록과 totalPages 0, last=true를 반환한다`() {
            whenever(postRepository.countMyPosts(eq(1L), anyOrNull())).thenReturn(0L)
            whenever(postRepository.findMyPosts(eq(1L), anyOrNull(), eq(0L), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            val result = postService.getMyPostsByOffset(1L, null, 0, 20)

            assertEquals(0, result.data.size)
            assertEquals(0L, result.totalCount)
            assertEquals(0L, result.totalPages)
            assertEquals(true, result.last)
        }

        @Test
        fun `section이 지정되면 해당 section으로 조회한다`() {
            whenever(postRepository.countMyPosts(eq(1L), eq(Section.INVEST))).thenReturn(0L)
            whenever(postRepository.findMyPosts(eq(1L), eq(Section.INVEST), eq(0L), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getMyPostsByOffset(1L, "invest", 0, 20)

            verify(postRepository).findMyPosts(eq(1L), eq(Section.INVEST), eq(0L), eq(20))
        }

        @Test
        fun `section이 없으면 전체(null)로 조회한다`() {
            whenever(postRepository.countMyPosts(eq(1L), isNull())).thenReturn(0L)
            whenever(postRepository.findMyPosts(eq(1L), isNull(), eq(0L), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getMyPostsByOffset(1L, null, 0, 20)

            verify(postRepository).findMyPosts(eq(1L), isNull(), eq(0L), eq(20))
        }

        @Test
        fun `page와 size로 offset을 계산하고 중간 페이지 메타를 반환한다`() {
            whenever(postRepository.countMyPosts(eq(1L), anyOrNull())).thenReturn(100L)
            whenever(postRepository.findMyPosts(eq(1L), anyOrNull(), eq(20L), eq(10))).thenReturn(emptyList())
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            val result = postService.getMyPostsByOffset(1L, null, 2, 10)

            verify(postRepository).findMyPosts(eq(1L), anyOrNull(), eq(20L), eq(10))
            assertEquals(10L, result.totalPages)
            assertEquals(2L, result.page)
            assertEquals(false, result.first)
            assertEquals(false, result.last)
        }

        @Test
        fun `size가 최대치를 넘으면 50으로 제한된다`() {
            whenever(postRepository.countMyPosts(eq(1L), anyOrNull())).thenReturn(0L)
            whenever(postRepository.findMyPosts(eq(1L), anyOrNull(), eq(0L), eq(50))).thenReturn(emptyList())
            whenever(userQueryClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getMyPostsByOffset(1L, null, 0, 100)

            verify(postRepository).findMyPosts(eq(1L), anyOrNull(), eq(0L), eq(50))
        }

        @Test
        fun `유효하지 않은 section이면 InvalidSectionException을 던진다`() {
            assertThrows<InvalidSectionException> {
                postService.getMyPostsByOffset(1L, "unknown", 0, 20)
            }

            verify(postRepository, never()).findMyPosts(any(), anyOrNull(), any(), any())
        }
    }
    */
}
