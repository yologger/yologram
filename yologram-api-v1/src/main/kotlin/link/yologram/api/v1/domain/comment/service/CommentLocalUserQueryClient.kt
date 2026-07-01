package link.yologram.api.v1.domain.comment.service

import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class CommentLocalUserQueryClient(
    private val userRepository: UserRepository,
) : UserQueryClient {

    override fun findNicknames(uids: Collection<Long>): Map<Long, String> {
        if (uids.isEmpty()) return emptyMap()
        return userRepository.findAllById(uids.toSet()).associate { it.id to it.nickname }
    }
}
