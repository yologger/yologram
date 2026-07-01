package link.yologram.api.v1.domain.comment.service

import link.yologram.api.v1.domain.comment.entity.Comment
import link.yologram.api.v1.domain.comment.exception.TargetPostNotFoundException
import link.yologram.api.v1.domain.comment.model.CreateCommentRequest
import link.yologram.api.v1.domain.comment.model.CreateCommentResponse
import link.yologram.api.v1.domain.comment.repository.CommentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postQueryClient: PostQueryClient,
) {

    // 댓글 작성
    @Transactional
    fun create(postId: Long, userId: Long, request: CreateCommentRequest): CreateCommentResponse {
        // 대상 글이 없으면 404 (고아 댓글 방지)
        if (!postQueryClient.exists(postId)) throw TargetPostNotFoundException()

        val comment = commentRepository.save(
            Comment(
                postId = postId,
                userId = userId,
                content = request.content!!,
            )
        )
        return CreateCommentResponse(id = comment.id)
    }
}
