package link.yologram.api.v1.infra.cache

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 인메모리 CacheService (레거시 미러) — 로컬 실험·테스트 대역용, 빈 미등록 (TTL 미적용).
 */
class LocalCacheService : CacheService {

    private val logger = KotlinLogging.logger {}

    private val store = HashMap<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> getOrNull(cache: Cache<V>): V? {
        return store[cache.key] as? V?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> getAllOrNull(caches: List<Cache<V>>): List<V>? {
        return caches.mapNotNull { cache -> store[cache.key] as? V? }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> getAllAsMap(caches: List<Cache<V>>): Map<String, V> {
        return caches.mapNotNull { cache -> (store[cache.key] as? V?)?.let { cache.key to it } }.toMap()
    }

    override fun <V : Any> set(cache: Cache<V>, value: V) {
        synchronized(store) {
            store[cache.key] = value
            logger.info { store }
        }
    }

    override fun <V : Any> setAll(caches: Map<Cache<V>, V>) {
        synchronized(store) {
            caches.map { (cache, value) -> store[cache.key] = value }
            logger.info { store }
        }
    }

    override fun deleteAll(vararg caches: Cache<*>) {
        synchronized(store) {
            caches.forEach { store -= it.key }
            logger.info { store }
        }
    }
}
