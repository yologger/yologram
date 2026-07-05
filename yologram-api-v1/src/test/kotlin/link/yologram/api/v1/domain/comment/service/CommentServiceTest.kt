package link.yologram.api.v1.domain.comment.service

import link.yologram.api.v1.domain.comment.entity.Comment
import link.yologram.api.v1.domain.comment.exception.CommentForbiddenException
import link.yologram.api.v1.domain.comment.exception.CommentNotFoundException
import link.yologram.api.v1.domain.comment.exception.TargetPostNotFoundException
import link.yologram.api.v1.domain.comment.model.CommentCursor
import link.yologram.api.v1.domain.comment.model.CommentSort
import link.yologram.api.v1.domain.comment.model.CreateCommentRequest
import link.yologram.api.v1.domain.comment.model.UpdateCommentRequest
import link.yologram.api.v1.domain.comment.repository.CommentRepository
import java.util.Optional
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class CommentServiceTest {

    @Mock
    lateinit var commentRepository: CommentRepository

    @Mock
    lateinit var postQueryClient: PostQueryClient

    @Mock
    lateinit var userQueryClient: UserQueryClient

    @InjectMocks
    lateinit var commentService: CommentService

    private fun comment(id: Long, postId: Long = 100L, userId: Long = 1L, content: String = "내용$id") =
        Comment(id = id, postId = postId, userId = userId, content = content)

    @Nested
    inner class 댓글_작성 {

        @Test
        fun `대상 글이 있으면 댓글을 저장하고 id를 반환한다`() {
            whenever(postQueryClient.exists(100L)).thenReturn(true)
            whenever(commentRepository.save(any<Comment>())).thenReturn(comment(10L))

            val result = commentService.create(100L, 1L, CreateCommentRequest(content = "좋은 글"))

            assertEquals(10L, result.id)
            verify(commentRepository).save(any<Comment>())
        }

        @Test
        fun `대상 글이 없으면 TargetPostNotFoundException을 던진다`() {
            whenever(postQueryClient.exists(999L)).thenReturn(false)

            assertThrows<TargetPostNotFoundException> {
                commentService.create(999L, 1L, CreateCommentRequest(content = "좋은 글"))
            }

            verify(commentRepository, never()).save(any<Comment>())
        }
    }

    @Nested
    inner class 댓글_수정 {

        @Test
        fun `본인 댓글이면 내용을 수정한다`() {
            val target = comment(1L, userId = 1L, content = "원본")
            whenever(commentRepository.findById(1L)).thenReturn(Optional.of(target))

            commentService.update(1L, 1L, UpdateCommentRequest(content = "수정됨"))

            assertEquals("수정됨", target.content)
        }

        @Test
        fun `존재하지 않는 댓글이면 CommentNotFoundException을 던진다`() {
            whenever(commentRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<CommentNotFoundException> {
                commentService.update(99L, 1L, UpdateCommentRequest(content = "수정"))
            }
        }

        @Test
        fun `본인 댓글이 아니면 CommentForbiddenException을 던진다`() {
            whenever(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, userId = 99L)))

            assertThrows<CommentForbiddenException> {
                commentService.update(1L, 1L, UpdateCommentRequest(content = "수정"))
            }
        }
    }

    @Nested
    inner class 댓글_삭제 {

        @Test
        fun `본인 댓글이면 삭제한다`() {
            val target = comment(1L, userId = 1L)
            whenever(commentRepository.findById(1L)).thenReturn(Optional.of(target))

            commentService.delete(1L, 1L)

            verify(commentRepository).delete(target)
        }

        @Test
        fun `존재하지 않는 댓글이면 CommentNotFoundException을 던진다`() {
            whenever(commentRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<CommentNotFoundException> {
                commentService.delete(99L, 1L)
            }

            verify(commentRepository, never()).delete(any<Comment>())
        }

        @Test
        fun `본인 댓글이 아니면 CommentForbiddenException을 던진다`() {
            whenever(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, userId = 99L)))

            assertThrows<CommentForbiddenException> {
                commentService.delete(1L, 1L)
            }

            verify(commentRepository, never()).delete(any<Comment>())
        }
    }

    @Nested
    inner class 댓글_목록_조회 {

        @Test
        fun `결과가 있으면 마지막 댓글 id를 nextCursor로 반환한다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(CommentSort.LATEST), isNull<Long>(), eq(2)))
                .thenReturn(listOf(comment(3L, userId = 3L), comment(2L, userId = 2L)))
            whenever(userQueryClient.findNicknames(any())).thenReturn(mapOf(3L to "u3", 2L to "u2"))

            val result = commentService.getCommentsByCursor(100L, null, null, 2)

            assertEquals(listOf(3L, 2L), result.data.map { it.id })
            assertEquals("u3", result.data[0].author.nickname)
            assertEquals(CommentCursor.encode(2L), result.nextCursor)
        }

        @Test
        fun `결과가 없으면 빈 목록과 null nextCursor를 반환한다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(CommentSort.LATEST), isNull<Long>(), eq(20)))
                .thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            val result = commentService.getCommentsByCursor(100L, null, null, 20)

            assertEquals(0, result.data.size)
            assertNull(result.nextCursor)
        }

        @Test
        fun `cursor가 주어지면 디코딩한 id로 조회한다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(CommentSort.LATEST), eq<Long?>(5L), eq(20)))
                .thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            commentService.getCommentsByCursor(100L, null, CommentCursor.encode(5L), 20)

            verify(commentRepository).findByPost(eq(100L), eq(CommentSort.LATEST), eq<Long?>(5L), eq(20))
        }

        @Test
        fun `sort=oldest면 OLDEST로 조회한다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(CommentSort.OLDEST), isNull<Long>(), eq(20)))
                .thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            commentService.getCommentsByCursor(100L, "oldest", null, 20)

            verify(commentRepository).findByPost(eq(100L), eq(CommentSort.OLDEST), isNull<Long>(), eq(20))
        }

        @Test
        fun `size가 최대치를 넘으면 50으로 제한된다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(CommentSort.LATEST), isNull<Long>(), eq(50)))
                .thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            commentService.getCommentsByCursor(100L, null, null, 100)

            verify(commentRepository).findByPost(eq(100L), eq(CommentSort.LATEST), isNull<Long>(), eq(50))
        }
    }

    // offset 엔드포인트는 비활성(CommentResource 주석)이라 학습용으로 테스트도 주석 처리
    /*
    @Nested
    inner class 댓글_목록_offset_학습용 {

        @Test
        fun `댓글 목록과 페이지 메타를 반환한다`() {
            whenever(commentRepository.countByPost(100L)).thenReturn(3L)
            whenever(commentRepository.findByPost(eq(100L), eq(CommentSort.LATEST), eq(0L), eq(20)))
                .thenReturn(listOf(comment(3L, userId = 3L), comment(2L, userId = 2L), comment(1L, userId = 1L)))
            whenever(userQueryClient.findNicknames(any())).thenReturn(mapOf(3L to "u3", 2L to "u2", 1L to "u1"))

            val result = commentService.getCommentsByOffset(100L, null, 0, 20)

            assertEquals(listOf(3L, 2L, 1L), result.data.map { it.id })
            assertEquals(3L, result.totalCount)
            assertEquals(1L, result.totalPages)
            assertEquals(true, result.first)
            assertEquals(true, result.last)
        }

        @Test
        fun `결과가 없으면 빈 목록과 totalPages 0, last=true를 반환한다`() {
            whenever(commentRepository.countByPost(100L)).thenReturn(0L)
            whenever(commentRepository.findByPost(eq(100L), eq(CommentSort.LATEST), eq(0L), eq(20))).thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            val result = commentService.getCommentsByOffset(100L, null, 0, 20)

            assertEquals(0, result.data.size)
            assertEquals(0L, result.totalPages)
            assertEquals(true, result.last)
        }

        @Test
        fun `page와 size로 offset을 계산한다`() {
            whenever(commentRepository.countByPost(100L)).thenReturn(100L)
            whenever(commentRepository.findByPost(eq(100L), eq(CommentSort.LATEST), eq(20L), eq(10))).thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            val result = commentService.getCommentsByOffset(100L, null, 2, 10)

            verify(commentRepository).findByPost(eq(100L), eq(CommentSort.LATEST), eq(20L), eq(10))
            assertEquals(10L, result.totalPages)
            assertEquals(2L, result.page)
            assertEquals(false, result.first)
            assertEquals(false, result.last)
        }
    }
    */
}
