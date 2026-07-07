package link.yologram.api.v1.domain.pms.service

import link.yologram.api.v1.domain.comment.repository.CommentRepository
import org.springframework.stereotype.Component

@Component
class LocalCommentCleanupClient(
    private val commentRepository: CommentRepository,
) : CommentCleanupClient {

    override fun deleteByPostId(postId: Long) {
        commentRepository.deleteByPostId(postId)
    }
}
