package link.yologram.api.v1.domain.pms.tech.service

import link.yologram.api.v1.domain.pms.tech.entity.TechPost
import link.yologram.api.v1.domain.pms.tech.entity.TechPostCategoryMapping
import link.yologram.api.v1.domain.pms.tech.exception.InvalidTechCategoryException
import link.yologram.api.v1.domain.pms.tech.exception.InvalidTechSectionException
import link.yologram.api.v1.domain.pms.tech.exception.TechPostForbiddenException
import link.yologram.api.v1.domain.pms.tech.exception.TechPostNotFoundException
import link.yologram.api.v1.domain.pms.tech.model.CreateTechPostRequest
import link.yologram.api.v1.domain.pms.tech.model.TechPostCursor
import link.yologram.api.v1.domain.pms.tech.model.TechPostWithCommentCount
import link.yologram.api.v1.domain.pms.tech.model.UpdateTechPostRequest
import link.yologram.api.v1.domain.pms.tech.repository.TechPostCategoryMappingRepository
import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import link.yologram.api.v1.infra.client.cms.CmsApiClient
import link.yologram.api.v1.infra.client.comment.CommentApiClient
import link.yologram.api.v1.infra.client.ums.UmsApiClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TechPostServiceTest {

    @Mock
    lateinit var postRepository: TechPostRepository

    @Mock
    lateinit var postCategoryMappingRepository: TechPostCategoryMappingRepository

    @Mock
    lateinit var cmsApiClient: CmsApiClient

    @Mock
    lateinit var umsApiClient: UmsApiClient

    @Mock
    lateinit var commentApiClient: CommentApiClient

    @InjectMocks
    lateinit var postService: TechPostService

    private fun savedPost(id: Long = 1L) =
        TechPost(id = id, userId = 1L, content = "내용")

    @Nested
    inner class 게시글_작성 {

        @Test
        fun `정상 작성 시 게시글과 카테고리를 저장하고 id를 반환한다`() {
            whenever(cmsApiClient.allActive(setOf(1L, 2L))).thenReturn(true)
            whenever(postRepository.save(any<TechPost>())).thenReturn(savedPost(10L))

            val result = postService.create(1L, CreateTechPostRequest(content = "내용", categoryIds = listOf(1L, 2L)))

            assertEquals(10L, result.id)
            verify(postCategoryMappingRepository, times(2)).save(any<TechPostCategoryMapping>())
        }

        @Test
        fun `카테고리가 테크 게시판 것이 아니면 InvalidTechCategoryException을 던진다`() {
            whenever(cmsApiClient.allActive(setOf(99L))).thenReturn(false)

            assertThrows<InvalidTechCategoryException> {
                postService.create(1L, CreateTechPostRequest(content = "내용", categoryIds = listOf(99L)))
            }

            verify(postRepository, never()).save(any<TechPost>())
        }
    }

    @Nested
    inner class 게시글_수정 {

        @Test
        fun `본인 글이면 제목·내용 수정 후 카테고리를 교체한다`() {
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(TechPost(id = 1L, userId = 1L, content = "원본")))
            whenever(cmsApiClient.allActive(setOf(2L, 3L))).thenReturn(true)

            postService.update(1L, 1L, UpdateTechPostRequest(title = "새 제목", content = "새 내용", categoryIds = listOf(2L, 3L)))

            verify(postCategoryMappingRepository).deleteByPostId(1L)
            verify(postCategoryMappingRepository, times(2)).save(any<TechPostCategoryMapping>())
        }

        @Test
        fun `존재하지 않는 글이면 TechPostNotFoundException을 던진다`() {
            whenever(postRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<TechPostNotFoundException> {
                postService.update(99L, 1L, UpdateTechPostRequest(content = "내용", categoryIds = listOf(1L)))
            }
        }

        @Test
        fun `본인 글이 아니면 TechPostForbiddenException을 던진다`() {
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(TechPost(id = 1L, userId = 99L, content = "내용")))

            assertThrows<TechPostForbiddenException> {
                postService.update(1L, 1L, UpdateTechPostRequest(content = "내용", categoryIds = listOf(1L)))
            }

            verify(postCategoryMappingRepository, never()).deleteByPostId(any())
        }

        @Test
        fun `카테고리가 테크 게시판 것이 아니면 InvalidTechCategoryException을 던진다`() {
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(TechPost(id = 1L, userId = 1L, content = "내용")))
            whenever(cmsApiClient.allActive(setOf(99L))).thenReturn(false)

            assertThrows<InvalidTechCategoryException> {
                postService.update(1L, 1L, UpdateTechPostRequest(content = "내용", categoryIds = listOf(99L)))
            }

            verify(postCategoryMappingRepository, never()).deleteByPostId(any())
        }
    }

    @Nested
    inner class 게시글_삭제 {

        @Test
        fun `본인 글이면 카테고리 매핑·댓글과 게시글을 삭제한다`() {
            val post = TechPost(id = 1L, userId = 1L, content = "내용")
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

            postService.delete(1L, 1L)

            verify(postCategoryMappingRepository).deleteByPostId(1L)
            verify(commentApiClient).deleteByPostId(1L)
            verify(postRepository).delete(post)
        }

        @Test
        fun `존재하지 않는 글이면 TechPostNotFoundException을 던진다`() {
            whenever(postRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<TechPostNotFoundException> {
                postService.delete(99L, 1L)
            }

            verify(postRepository, never()).delete(any<TechPost>())
            verify(commentApiClient, never()).deleteByPostId(any())
        }

        @Test
        fun `본인 글이 아니면 TechPostForbiddenException을 던진다`() {
            whenever(postRepository.findById(1L)).thenReturn(Optional.of(TechPost(id = 1L, userId = 99L, content = "내용")))

            assertThrows<TechPostForbiddenException> {
                postService.delete(1L, 1L)
            }

            verify(postCategoryMappingRepository, never()).deleteByPostId(any())
            verify(commentApiClient, never()).deleteByPostId(any())
            verify(postRepository, never()).delete(any<TechPost>())
        }
    }

    @Nested
    inner class 게시글_상세_조회 {

        @Test
        fun `게시글과 카테고리·작성자 닉네임·댓글 수를 반환한다`() {
            val post = TechPost(id = 1L, userId = 12L, title = "제목", content = "내용", likeCount = 3)
            // 댓글 수는 tech_post_comment_count leftJoin 프로젝션에서 온다 (엔티티 컬럼 아님)
            whenever(postRepository.findPostWithCommentCount(1L)).thenReturn(TechPostWithCommentCount(post, 2L))
            whenever(postCategoryMappingRepository.findByPostId(1L)).thenReturn(
                listOf(TechPostCategoryMapping(id = 1L, postId = 1L, categoryId = 1L), TechPostCategoryMapping(id = 2L, postId = 1L, categoryId = 2L)),
            )
            whenever(umsApiClient.findNickname(12L)).thenReturn("tester")

            val result = postService.getPost(1L)

            assertEquals(1L, result.id)
            assertEquals("TECH", result.section)
            assertEquals(12L, result.author.uid)
            assertEquals("tester", result.author.nickname)
            assertEquals(listOf(1L, 2L), result.categoryIds)
            assertEquals("내용", result.content)
            assertEquals(2, result.commentCount)
        }

        @Test
        fun `존재하지 않는 게시글이면 TechPostNotFoundException을 던진다`() {
            whenever(postRepository.findPostWithCommentCount(99L)).thenReturn(null)

            assertThrows<TechPostNotFoundException> {
                postService.getPost(99L)
            }
        }
    }

    @Nested
    inner class 게시글_목록_조회 {

        private fun post(id: Long, userId: Long = id, commentCount: Long = 0L) =
            TechPostWithCommentCount(TechPost(id = id, userId = userId, content = "내용$id"), commentCount)

        @Test
        fun `결과가 있으면 마지막 글 id를 nextCursor로 반환한다`() {
            // 댓글 수는 leftJoin 프로젝션 값 — 글 3은 5개, 글 2는 count row 없음(coalesce 0)
            val fetched = listOf(post(3, commentCount = 5L), post(2))
            whenever(postRepository.findPosts(anyOrNull(), isNull<Long>(), eq(2))).thenReturn(fetched)
            whenever(umsApiClient.findNicknames(any())).thenReturn(mapOf(3L to "u3", 2L to "u2"))
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(
                listOf(TechPostCategoryMapping(id = 1L, postId = 3L, categoryId = 10L)),
            )

            val result = postService.getPostsByCursor(null, null, 2)

            assertEquals(2, result.data.size)
            assertEquals(listOf(3L, 2L), result.data.map { it.id })
            assertEquals(listOf(10L), result.data[0].categoryIds)
            assertEquals("u3", result.data[0].author.nickname)
            assertEquals("TECH", result.data[0].section)
            assertEquals(listOf(5, 0), result.data.map { it.commentCount })
            // 마지막 글 id(2)를 인코딩한 값
            assertEquals(TechPostCursor.encode(2L), result.nextCursor)
        }

        @Test
        fun `결과가 없으면 빈 목록과 null nextCursor를 반환한다`() {
            whenever(postRepository.findPosts(anyOrNull(), isNull<Long>(), eq(20))).thenReturn(emptyList())
            whenever(umsApiClient.findNicknames(any())).thenReturn(emptyMap())
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            val result = postService.getPostsByCursor(null, null, 20)

            assertEquals(0, result.data.size)
            assertNull(result.nextCursor)
        }

        @Test
        fun `cursor가 주어지면 디코딩한 id로 조회한다`() {
            whenever(postRepository.findPosts(anyOrNull(), eq<Long?>(5L), eq(20))).thenReturn(emptyList())
            whenever(umsApiClient.findNicknames(any())).thenReturn(emptyMap())
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getPostsByCursor(null, TechPostCursor.encode(5L), 20)

            verify(postRepository).findPosts(anyOrNull(), eq<Long?>(5L), eq(20))
        }

        @Test
        fun `size가 최대치를 넘으면 50으로 제한된다`() {
            whenever(postRepository.findPosts(anyOrNull(), isNull<Long>(), eq(50))).thenReturn(emptyList())
            whenever(umsApiClient.findNicknames(any())).thenReturn(emptyMap())
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getPostsByCursor(null, null, 100)

            verify(postRepository).findPosts(anyOrNull(), isNull<Long>(), eq(50))
        }
    }

    @Nested
    inner class 내_글_목록_cursor {

        private fun post(id: Long, commentCount: Long = 0L) =
            TechPostWithCommentCount(TechPost(id = id, userId = 1L, content = "내용$id"), commentCount)

        @Test
        fun `결과가 있으면 마지막 글 id를 nextCursor로 반환한다`() {
            whenever(postRepository.findMyPosts(eq(1L), isNull<Long>(), eq(2)))
                .thenReturn(listOf(post(3, commentCount = 7L), post(2)))
            whenever(umsApiClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(
                listOf(TechPostCategoryMapping(id = 1L, postId = 3L, categoryId = 10L)),
            )

            val result = postService.getMyPostsByCursor(1L, null, null, 2)

            assertEquals(listOf(3L, 2L), result.data.map { it.id })
            assertEquals("me", result.data[0].author.nickname)
            assertEquals(listOf(10L), result.data[0].categoryIds)
            assertEquals(listOf(7, 0), result.data.map { it.commentCount })
            assertEquals(TechPostCursor.encode(2L), result.nextCursor)
        }

        @Test
        fun `결과가 없으면 빈 목록과 null nextCursor를 반환한다`() {
            whenever(postRepository.findMyPosts(eq(1L), isNull<Long>(), eq(20))).thenReturn(emptyList())
            whenever(umsApiClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            val result = postService.getMyPostsByCursor(1L, null, null, 20)

            assertEquals(0, result.data.size)
            assertNull(result.nextCursor)
        }

        @Test
        fun `cursor가 주어지면 디코딩한 id로 조회한다`() {
            whenever(postRepository.findMyPosts(eq(1L), eq<Long?>(5L), eq(20))).thenReturn(emptyList())
            whenever(umsApiClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getMyPostsByCursor(1L, null, TechPostCursor.encode(5L), 20)

            verify(postRepository).findMyPosts(eq(1L), eq<Long?>(5L), eq(20))
        }

        @Test
        fun `section이 tech면 정상 조회한다 (구 API 호환)`() {
            whenever(postRepository.findMyPosts(eq(1L), isNull<Long>(), eq(20))).thenReturn(emptyList())
            whenever(umsApiClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getMyPostsByCursor(1L, "tech", null, 20)

            verify(postRepository).findMyPosts(eq(1L), isNull<Long>(), eq(20))
        }

        @Test
        fun `section은 대소문자를 구분하지 않는다`() {
            whenever(postRepository.findMyPosts(eq(1L), isNull<Long>(), eq(20))).thenReturn(emptyList())
            whenever(umsApiClient.findNickname(1L)).thenReturn("me")
            whenever(postCategoryMappingRepository.findByPostIdIn(any())).thenReturn(emptyList())

            postService.getMyPostsByCursor(1L, "TECH", null, 20)

            verify(postRepository).findMyPosts(eq(1L), isNull<Long>(), eq(20))
        }

        @Test
        fun `tech가 아닌 section이면 InvalidTechSectionException을 던진다`() {
            assertThrows<InvalidTechSectionException> {
                postService.getMyPostsByCursor(1L, "unknown", null, 20)
            }

            verify(postRepository, never()).findMyPosts(any(), isNull<Long>(), any())
        }
    }
}
