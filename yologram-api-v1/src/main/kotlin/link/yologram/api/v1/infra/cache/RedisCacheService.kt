package link.yologram.api.v1.infra.cache

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

/**
 * StringRedisTemplate + Jackson 기반 CacheService (레거시 미러).
 * 캐시는 보조 수단이므로 전 연산 runCatching — Redis 장애 시 로그만 남기고
 * 미스(null/빈 결과)로 처리해 호출부가 DB로 폴백하게 한다.
 */
@Service
class RedisCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : CacheService {

    private val logger = KotlinLogging.logger {}

    override fun <V : Any> getOrNull(cache: Cache<V>): V? =
        runCatching {
            val json = redisTemplate.opsForValue().get(cache.key)
            if (json.isNullOrBlank()) null
            else objectMapper.readValue(json, cache.type)
        }.onFailure {
            logger.error(it) { "unexpected error occurred while reading data from redis" }
        }.getOrNull()

    override fun <V : Any> getAllOrNull(caches: List<Cache<V>>): List<V>? =
        caches.takeIf { it.isNotEmpty() }?.let { list ->
            val keys = list.map { it.key }
            val type = list.first().type
            runCatching {
                val jsons = redisTemplate.opsForValue().multiGet(keys)?.filterNotNull()
                if (jsons.isNullOrEmpty()) null
                else jsons.map { objectMapper.readValue(it, type) }
            }.onFailure {
                logger.error(it) { "unexpected error occurred while reading data from redis" }
            }.getOrNull()
        }

    override fun <V : Any> getAllAsMap(caches: List<Cache<V>>): Map<String, V> {
        if (caches.isEmpty()) return emptyMap()
        val keys = caches.map { it.key }
        val type = caches.first().type
        return runCatching {
            // MGET은 키 순서대로 값/null을 돌려주므로 키와 zip해 미스(null)를 제외한 맵 구성
            val jsons = redisTemplate.opsForValue().multiGet(keys) ?: return emptyMap()
            keys.zip(jsons)
                .mapNotNull { (key, json) ->
                    if (json.isNullOrBlank()) null
                    else key to objectMapper.readValue(json, type)
                }
                .toMap()
        }.onFailure {
            logger.error(it) { "unexpected error occurred while reading data from redis" }
        }.getOrDefault(emptyMap()) // 실패는 전체 미스로 취급 → 호출부 DB 폴백
    }

    override fun <V : Any> set(cache: Cache<V>, value: V) {
        runCatching {
            val json = objectMapper.writeValueAsString(value)
            redisTemplate.opsForValue()
                .set(cache.key, json, cache.duration)
        }.onFailure {
            logger.error(it) { "unexpected error occurred while saving data to redis" }
        }
    }

    override fun <V : Any> setAll(caches: Map<Cache<V>, V>) {
        runCatching {
            caches.forEach { (cache, value) ->
                val json = objectMapper.writeValueAsString(value)
                redisTemplate.opsForValue()
                    .set(cache.key, json, cache.duration)
            }
        }.onFailure {
            logger.error(it) { "unexpected error occurred while saving data to redis" }
        }
    }

    override fun deleteAll(vararg caches: Cache<*>) {
        runCatching {
            val keys = caches.map { it.key }
            redisTemplate.delete(keys)
        }.onFailure {
            logger.error(it) { "unexpected error occurred while deleting data from redis" }
        }
    }
}
