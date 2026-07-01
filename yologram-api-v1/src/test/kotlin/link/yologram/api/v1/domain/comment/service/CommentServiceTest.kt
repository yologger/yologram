package link.yologram.api.v1.domain.comment.service

import link.yologram.api.v1.domain.comment.entity.Comment
import link.yologram.api.v1.domain.comment.exception.TargetPostNotFoundException
import link.yologram.api.v1.domain.comment.model.CreateCommentRequest
import link.yologram.api.v1.domain.comment.repository.CommentRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class CommentServiceTest {

    @Mock
    lateinit var commentRepository: CommentRepository

    @Mock
    lateinit var postQueryClient: PostQueryClient

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
}
