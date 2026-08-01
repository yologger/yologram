package link.yologram.api.v1.domain.pms.tech.service

import link.yologram.api.v1.domain.comment.tech.repository.TechPostCommentRepository
import org.springframework.stereotype.Component

@Component
class LocalTechPostCommentCleanupClient(
    private val commentRepository: TechPostCommentRepository,
) : TechPostCommentCleanupClient {

    override fun deleteByPostId(postId: Long) {
        commentRepository.deleteByPostId(postId)
    }
}
