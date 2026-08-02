package link.yologram.api.v1.infra.cache

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.TimeUnit

@Testcontainers
class RedisCacheServiceTest {

    companion object {
        // prod은 Valkey(ElastiCache) — 동일 계열 이미지로 검증 (RESP 호환)
        @Container
        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("valkey/valkey:8-alpine"))
            .withExposedPorts(6379)

        lateinit var connectionFactory: LettuceConnectionFactory
        lateinit var redisTemplate: StringRedisTemplate
        lateinit var cacheService: RedisCacheService

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            connectionFactory = LettuceConnectionFactory(redis.host, redis.getMappedPort(6379))
            connectionFactory.afterPropertiesSet()
            redisTemplate = StringRedisTemplate(connectionFactory)
            redisTemplate.afterPropertiesSet()
            cacheService = RedisCacheService(redisTemplate, jacksonObjectMapper())
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            connectionFactory.destroy()
        }
    }

    @BeforeEach
    fun flush() {
        connectionFactory.connection.serverCommands().flushAll()
    }

    @Nested
    inner class 단건_저장_조회 {

        @Test
        fun `set 후 getOrNull로 조회된다`() {
            val cache = Cache.userNickname(1L)

            cacheService.set(cache, "요로거")

            assertEquals("요로거", cacheService.getOrNull(cache))
        }

        @Test
        fun `없는 키는 null을 반환한다`() {
            assertNull(cacheService.getOrNull(Cache.userNickname(999L)))
        }

        @Test
        fun `set 시 TTL이 설정된다`() {
            val cache = Cache.userNickname(1L)

            cacheService.set(cache, "요로거")

            val ttl = redisTemplate.getExpire(cache.key, TimeUnit.SECONDS)
            assertTrue(ttl in 1..cache.duration.seconds) { "TTL은 1~${cache.duration.seconds}초 사이여야 한다: $ttl" }
        }
    }

    @Nested
    inner class 배치_조회 {

        @Test
        fun `전체 히트 시 모든 키의 값을 반환한다`() {
            cacheService.setAll(
                mapOf(
                    Cache.userNickname(1L) to "닉1",
                    Cache.userNickname(2L) to "닉2",
                )
            )

            val result = cacheService.getAllAsMap(listOf(Cache.userNickname(1L), Cache.userNickname(2L)))

            assertEquals(
                mapOf(
                    Cache.userNickname(1L).key to "닉1",
                    Cache.userNickname(2L).key to "닉2",
                ),
                result,
            )
        }

        @Test
        fun `부분 히트 시 미스 키는 맵에서 제외된다`() {
            cacheService.set(Cache.userNickname(1L), "닉1")

            val result = cacheService.getAllAsMap(
                listOf(Cache.userNickname(1L), Cache.userNickname(2L), Cache.userNickname(3L))
            )

            assertEquals(mapOf(Cache.userNickname(1L).key to "닉1"), result)
        }

        @Test
        fun `전체 미스면 빈 맵을 반환한다`() {
            val result = cacheService.getAllAsMap(listOf(Cache.userNickname(1L), Cache.userNickname(2L)))

            assertTrue(result.isEmpty())
        }

        @Test
        fun `빈 리스트면 빈 맵을 반환한다`() {
            assertTrue(cacheService.getAllAsMap(emptyList<Cache<String>>()).isEmpty())
        }

        @Test
        fun `레거시 getAllOrNull은 히트된 값 리스트만 반환한다`() {
            cacheService.set(Cache.userNickname(1L), "닉1")

            val result = cacheService.getAllOrNull(listOf(Cache.userNickname(1L), Cache.userNickname(2L)))

            assertEquals(listOf("닉1"), result)
        }
    }

    @Nested
    inner class 삭제 {

        @Test
        fun `deleteAll 후 조회하면 null이다`() {
            cacheService.set(Cache.userNickname(1L), "닉1")
            cacheService.set(Cache.userNickname(2L), "닉2")

            cacheService.deleteAll(Cache.userNickname(1L), Cache.userNickname(2L))

            assertNull(cacheService.getOrNull(Cache.userNickname(1L)))
            assertNull(cacheService.getOrNull(Cache.userNickname(2L)))
        }
    }

    @Nested
    inner class Redis_장애 {

        private val brokenTemplate = mock<StringRedisTemplate>().also {
            whenever(it.opsForValue()).thenThrow(RuntimeException("connection refused"))
            whenever(it.delete(any<List<String>>())).thenThrow(RuntimeException("connection refused"))
        }
        private val brokenService = RedisCacheService(brokenTemplate, jacksonObjectMapper())

        @Test
        fun `getOrNull은 예외 시 null을 반환한다`() {
            assertNull(brokenService.getOrNull(Cache.userNickname(1L)))
        }

        @Test
        fun `getAllAsMap은 예외 시 빈 맵을 반환한다`() {
            assertTrue(brokenService.getAllAsMap(listOf(Cache.userNickname(1L))).isEmpty())
        }

        @Test
        fun `set은 예외를 전파하지 않는다`() {
            assertDoesNotThrow { brokenService.set(Cache.userNickname(1L), "닉1") }
        }

        @Test
        fun `setAll은 예외를 전파하지 않는다`() {
            assertDoesNotThrow { brokenService.setAll(mapOf(Cache.userNickname(1L) to "닉1")) }
        }

        @Test
        fun `deleteAll은 예외를 전파하지 않는다`() {
            assertDoesNotThrow { brokenService.deleteAll(Cache.userNickname(1L)) }
        }
    }
}
