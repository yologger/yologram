package link.yologram.api.v1.domain.comment.service

import link.yologram.api.v1.domain.pms.repository.PostRepository
import org.springframework.stereotype.Component

@Component
class LocalPostQueryClient(
    private val postRepository: PostRepository,
) : PostQueryClient {

    override fun exists(postId: Long): Boolean {
        return postRepository.existsById(postId)
    }
}
