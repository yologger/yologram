package link.yologram.api.v1.domain.comment.tech.service

import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import org.springframework.stereotype.Component

@Component
class LocalTechPostQueryClient(
    private val postRepository: TechPostRepository,
) : TechPostQueryClient {

    override fun exists(postId: Long): Boolean {
        return postRepository.existsById(postId)
    }
}
