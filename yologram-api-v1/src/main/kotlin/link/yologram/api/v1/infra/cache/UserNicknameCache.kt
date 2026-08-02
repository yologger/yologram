package link.yologram.api.v1.infra.cache

import org.springframework.stereotype.Component

/**
 * 유저 닉네임 cache-aside 공용 컴포넌트.
 * pms·comment 양쪽 LocalUmsApiClient에 캐시 로직이 중복되는 것을 막기 위해 infra로 뺐다 —
 * DB 조회는 loader 람다로 주입받아 도메인 경계(리포지토리 소유)는 각 도메인에 남긴다 (호출부 서비스 무변경).
 */
@Component
class UserNicknameCache(
    private val cacheService: CacheService,
) {

    /** 단건 cache-aside: 히트 시 반환, 미스 시 loader → 결과 캐시 (null이면 미캐시). */
    fun getNickname(uid: Long, loader: (Long) -> String?): String? {
        val cache = Cache.userNickname(uid)
        cacheService.getOrNull(cache)?.let { return it }

        val loaded = loader(uid) ?: return null
        cacheService.set(cache, loaded)
        return loaded
    }

    /**
     * 배치 cache-aside: getAllAsMap으로 히트 분리 → 미스 uid만 loader(IN 조회) → setAll로 채움 → 병합.
     * Redis 장애 시 getAllAsMap이 빈 맵(전체 미스)을 돌려주므로 자연스럽게 전체 DB 폴백된다.
     */
    fun getNicknames(uids: Collection<Long>, loader: (Collection<Long>) -> Map<Long, String>): Map<Long, String> {
        if (uids.isEmpty()) return emptyMap()

        val distinctUids = uids.toSet()
        val cacheByUid = distinctUids.associateWith { Cache.userNickname(it) }
        val hitByKey = cacheService.getAllAsMap(cacheByUid.values.toList())
        val hits = cacheByUid.entries
            .mapNotNull { (uid, cache) -> hitByKey[cache.key]?.let { uid to it } }
            .toMap()

        val missedUids = distinctUids - hits.keys
        if (missedUids.isEmpty()) return hits

        val loaded = loader(missedUids)
        if (loaded.isNotEmpty()) {
            cacheService.setAll(loaded.entries.associate { (uid, nickname) -> Cache.userNickname(uid) to nickname })
        }
        return hits + loaded
    }
}
