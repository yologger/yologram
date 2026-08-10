package link.yologram.api.v1.infra.client.pms

import link.yologram.api.v1.domain.pms.tech.repository.TechPostCommentCountRepository
import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import org.springframework.stereotype.Component

/** 타 도메인 리포지토리(pms TechPostRepository 등) import는 infra/client 층에서만 허용 — 도메인 간 참조 격리 지점 */
@Component
class LocalPmsApiClient(
    private val postRepository: TechPostRepository,
    private val postCommentCountRepository: TechPostCommentCountRepository,
) : PmsApiClient {

    override fun exists(postId: Long): Boolean {
        return postRepository.existsById(postId)
    }

    override fun increasePostCommentCount(postId: Long) {
        postCommentCountRepository.increase(postId)
    }

    override fun decreasePostCommentCount(postId: Long) {
        postCommentCountRepository.decrease(postId)
    }
}
