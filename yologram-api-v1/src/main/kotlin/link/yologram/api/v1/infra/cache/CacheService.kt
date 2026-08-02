package link.yologram.api.v1.infra.cache

interface CacheService {

    fun <V : Any> getOrNull(cache: Cache<V>): V?

    fun <V : Any> getAllOrNull(caches: List<Cache<V>>): List<V>?

    /**
     * 배치 조회 (MGET) — key→value 맵으로 반환하고 미스 키는 제외한다.
     * 레거시 getAllOrNull은 값 리스트만 돌려줘 미스가 어느 키인지 알 수 없어
     * 부분 히트 판별이 필요한 배치 캐시(cache-aside)에 부적합 — 그 보완으로 추가.
     */
    fun <V : Any> getAllAsMap(caches: List<Cache<V>>): Map<String, V>

    fun <V : Any> set(cache: Cache<V>, value: V)

    fun <V : Any> setAll(caches: Map<Cache<V>, V>)

    fun deleteAll(vararg caches: Cache<*>)
}
