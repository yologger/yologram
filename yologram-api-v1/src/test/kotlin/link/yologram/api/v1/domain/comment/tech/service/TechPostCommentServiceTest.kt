package link.yologram.api.v1.domain.comment.tech.service

import link.yologram.api.v1.domain.comment.tech.entity.TechPostComment
import link.yologram.api.v1.domain.comment.tech.exception.TargetTechPostNotFoundException
import link.yologram.api.v1.domain.comment.tech.exception.TechPostCommentForbiddenException
import link.yologram.api.v1.domain.comment.tech.exception.TechPostCommentNotFoundException
import link.yologram.api.v1.domain.comment.tech.model.CreateTechPostCommentRequest
import link.yologram.api.v1.domain.comment.tech.model.TechPostCommentCursor
import link.yologram.api.v1.domain.comment.tech.model.TechPostCommentSort
import link.yologram.api.v1.domain.comment.tech.model.UpdateTechPostCommentRequest
import link.yologram.api.v1.domain.comment.tech.repository.TechPostCommentRepository
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TechPostCommentServiceTest {

    @Mock
    lateinit var commentRepository: TechPostCommentRepository

    @Mock
    lateinit var postQueryClient: TechPostQueryClient

    @Mock
    lateinit var userQueryClient: UserQueryClient

    @InjectMocks
    lateinit var commentService: TechPostCommentService

    private fun comment(id: Long, postId: Long = 100L, userId: Long = 1L, content: String = "내용$id") =
        TechPostComment(id = id, postId = postId, userId = userId, content = content)

    @Nested
    inner class 댓글_작성 {

        @Test
        fun `대상 글이 있으면 댓글을 저장하고 id를 반환한다`() {
            whenever(postQueryClient.exists(100L)).thenReturn(true)
            whenever(commentRepository.save(any<TechPostComment>())).thenReturn(comment(10L))

            val result = commentService.create(100L, 1L, CreateTechPostCommentRequest(content = "좋은 글"))

            assertEquals(10L, result.id)
            verify(commentRepository).save(any<TechPostComment>())
        }

        @Test
        fun `대상 글이 없으면 TargetTechPostNotFoundException을 던진다`() {
            whenever(postQueryClient.exists(999L)).thenReturn(false)

            assertThrows<TargetTechPostNotFoundException> {
                commentService.create(999L, 1L, CreateTechPostCommentRequest(content = "좋은 글"))
            }

            verify(commentRepository, never()).save(any<TechPostComment>())
        }
    }

    @Nested
    inner class 댓글_수정 {

        @Test
        fun `본인 댓글이면 내용을 수정한다`() {
            val target = comment(1L, userId = 1L, content = "원본")
            whenever(commentRepository.findById(1L)).thenReturn(Optional.of(target))

            commentService.update(1L, 1L, UpdateTechPostCommentRequest(content = "수정됨"))

            assertEquals("수정됨", target.content)
        }

        @Test
        fun `존재하지 않는 댓글이면 TechPostCommentNotFoundException을 던진다`() {
            whenever(commentRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<TechPostCommentNotFoundException> {
                commentService.update(99L, 1L, UpdateTechPostCommentRequest(content = "수정"))
            }
        }

        @Test
        fun `본인 댓글이 아니면 TechPostCommentForbiddenException을 던진다`() {
            whenever(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, userId = 99L)))

            assertThrows<TechPostCommentForbiddenException> {
                commentService.update(1L, 1L, UpdateTechPostCommentRequest(content = "수정"))
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
        fun `존재하지 않는 댓글이면 TechPostCommentNotFoundException을 던진다`() {
            whenever(commentRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<TechPostCommentNotFoundException> {
                commentService.delete(99L, 1L)
            }

            verify(commentRepository, never()).delete(any<TechPostComment>())
        }

        @Test
        fun `본인 댓글이 아니면 TechPostCommentForbiddenException을 던진다`() {
            whenever(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, userId = 99L)))

            assertThrows<TechPostCommentForbiddenException> {
                commentService.delete(1L, 1L)
            }

            verify(commentRepository, never()).delete(any<TechPostComment>())
        }
    }

    @Nested
    inner class 댓글_목록_조회 {

        @Test
        fun `결과가 있으면 마지막 댓글 id를 nextCursor로 반환한다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(TechPostCommentSort.LATEST), isNull<Long>(), eq(2)))
                .thenReturn(listOf(comment(3L, userId = 3L), comment(2L, userId = 2L)))
            whenever(userQueryClient.findNicknames(any())).thenReturn(mapOf(3L to "u3", 2L to "u2"))

            val result = commentService.getCommentsByCursor(100L, null, null, 2)

            assertEquals(listOf(3L, 2L), result.data.map { it.id })
            assertEquals("u3", result.data[0].author.nickname)
            assertEquals(TechPostCommentCursor.encode(2L), result.nextCursor)
        }

        @Test
        fun `결과가 없으면 빈 목록과 null nextCursor를 반환한다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(TechPostCommentSort.LATEST), isNull<Long>(), eq(20)))
                .thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            val result = commentService.getCommentsByCursor(100L, null, null, 20)

            assertEquals(0, result.data.size)
            assertNull(result.nextCursor)
        }

        @Test
        fun `cursor가 주어지면 디코딩한 id로 조회한다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(TechPostCommentSort.LATEST), eq<Long?>(5L), eq(20)))
                .thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            commentService.getCommentsByCursor(100L, null, TechPostCommentCursor.encode(5L), 20)

            verify(commentRepository).findByPost(eq(100L), eq(TechPostCommentSort.LATEST), eq<Long?>(5L), eq(20))
        }

        @Test
        fun `sort=oldest면 OLDEST로 조회한다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(TechPostCommentSort.OLDEST), isNull<Long>(), eq(20)))
                .thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            commentService.getCommentsByCursor(100L, "oldest", null, 20)

            verify(commentRepository).findByPost(eq(100L), eq(TechPostCommentSort.OLDEST), isNull<Long>(), eq(20))
        }

        @Test
        fun `size가 최대치를 넘으면 50으로 제한된다`() {
            whenever(commentRepository.findByPost(eq(100L), eq(TechPostCommentSort.LATEST), isNull<Long>(), eq(50)))
                .thenReturn(emptyList())
            whenever(userQueryClient.findNicknames(any())).thenReturn(emptyMap())

            commentService.getCommentsByCursor(100L, null, null, 100)

            verify(commentRepository).findByPost(eq(100L), eq(TechPostCommentSort.LATEST), isNull<Long>(), eq(50))
        }
    }
}
