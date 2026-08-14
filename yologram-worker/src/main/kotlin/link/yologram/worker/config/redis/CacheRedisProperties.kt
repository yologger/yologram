package link.yologram.worker.config.redis

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 캐시용 Redis(Valkey) 접속 정보 — spring.data.redis 자동구성 대신 커스텀 프리픽스 사용.
 * DataSource(database.main.*)와 동일한 패턴: 자동구성 exclude + 커스텀 프로퍼티 + 수동 빈(RedisConfig).
 * (api-v1 CacheRedisProperties 미러)
 */
@ConfigurationProperties(prefix = "cache.data.redis")
data class CacheRedisProperties(
    val host: String,
    val port: Int = 6379,
)
