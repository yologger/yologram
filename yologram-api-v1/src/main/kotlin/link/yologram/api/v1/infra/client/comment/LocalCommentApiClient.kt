package link.yologram.api.v1.infra.client.comment

import link.yologram.api.v1.domain.comment.tech.repository.TechPostCommentRepository
import org.springframework.stereotype.Component

/** 타 도메인 리포지토리(comment TechPostCommentRepository) import는 infra/client 층에서만 허용 — 도메인 간 참조 격리 지점 */
@Component
class LocalCommentApiClient(
    private val commentRepository: TechPostCommentRepository,
) : CommentApiClient {

    override fun deleteByPostId(postId: Long) {
        commentRepository.deleteByPostId(postId)
    }
}
