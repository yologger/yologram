package link.yologram.api.v1.domain.tech.post.service

import link.yologram.api.v1.domain.tech.comment.repository.TechPostCommentRepository
import org.springframework.stereotype.Component

@Component
class LocalTechPostCommentCleanupClient(
    private val commentRepository: TechPostCommentRepository,
) : TechPostCommentCleanupClient {

    override fun deleteByPostId(postId: Long) {
        commentRepository.deleteByPostId(postId)
    }
}
