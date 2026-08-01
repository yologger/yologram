package link.yologram.api.v1.domain.comment.tech.service

import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.springframework.stereotype.Component

// 클래스명에 도메인 접두를 붙여 tech/post의 LocalUserQueryClient와 빈 이름 충돌을 피한다
@Component
class TechPostCommentLocalUserQueryClient(
    private val userRepository: UserRepository,
) : UserQueryClient {

    override fun findNicknames(uids: Collection<Long>): Map<Long, String> {
        if (uids.isEmpty()) return emptyMap()
        return userRepository.findAllById(uids.toSet()).associate { it.id to it.nickname }
    }
}
