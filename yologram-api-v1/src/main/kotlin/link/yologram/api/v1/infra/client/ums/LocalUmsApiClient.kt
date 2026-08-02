package link.yologram.api.v1.infra.client.ums

import link.yologram.api.v1.domain.ums.repository.UserRepository
import link.yologram.api.v1.infra.cache.UserNicknameCache
import org.springframework.stereotype.Component

/**
 * 타 도메인 리포지토리(ums UserRepository) import는 infra/client 층에서만 허용 — 도메인 간 참조 격리 지점.
 * pms·comment에 중복돼 있던 구현을 통합해 닉네임 cache-aside(UserNicknameCache)도 이 한 곳에서 적용된다.
 */
@Component
class LocalUmsApiClient(
    private val userRepository: UserRepository,
    private val userNicknameCache: UserNicknameCache,
) : UmsApiClient {

    override fun findNickname(uid: Long): String? {
        return userNicknameCache.getNickname(uid) { missedUid ->
            userRepository.findById(missedUid).map { it.nickname }.orElse(null)
        }
    }

    override fun findNicknames(uids: Collection<Long>): Map<Long, String> {
        if (uids.isEmpty()) return emptyMap()
        return userNicknameCache.getNicknames(uids) { missedUids ->
            userRepository.findAllById(missedUids.toSet()).associate { it.id to it.nickname }
        }
    }
}
