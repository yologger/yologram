package link.yologram.api.v1.infra.client.pms

import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import org.springframework.stereotype.Component

/** 타 도메인 리포지토리(pms TechPostRepository) import는 infra/client 층에서만 허용 — 도메인 간 참조 격리 지점 */
@Component
class LocalPmsApiClient(
    private val postRepository: TechPostRepository,
) : PmsApiClient {

    override fun exists(postId: Long): Boolean {
        return postRepository.existsById(postId)
    }
}
