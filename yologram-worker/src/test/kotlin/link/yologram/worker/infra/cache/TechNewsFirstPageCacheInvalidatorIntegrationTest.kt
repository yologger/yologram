package link.yologram.worker.infra.cache

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * 실제 Redis(Valkey 컨테이너)에서 UNLINK 전수 열거 무효화를 검증 — mock으로는 못 보는
 * "API가 쓴 키가 실제로 지워지는가"(api-v1 Cache.kt 키 스킴과의 문자열 계약)를 통합 확인.
 * api-v1 RedisCacheServiceTest와 동일 패턴(컨테이너 직결, 스프링 컨텍스트 없음).
 */
@Testcontainers
class TechNewsFirstPageCacheInvalidatorIntegrationTest {

    companion object {
        // prod은 Valkey(ElastiCache) — 동일 계열 이미지로 검증 (RESP 호환)
        @Container
        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("valkey/valkey:8-alpine"))
            .withExposedPorts(6379)

        lateinit var connectionFactory: LettuceConnectionFactory
        lateinit var redisTemplate: StringRedisTemplate
        lateinit var invalidator: TechNewsFirstPageCacheInvalidator

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            connectionFactory = LettuceConnectionFactory(redis.host, redis.getMappedPort(6379))
            connectionFactory.afterPropertiesSet()
            redisTemplate = StringRedisTemplate(connectionFactory)
            redisTemplate.afterPropertiesSet()
            invalidator = TechNewsFirstPageCacheInvalidator(redisTemplate)
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

    /** API(v1·v2)가 저장하는 것과 동일한 키 문자열로 직접 SET — 키 계약 검증의 핵심 */
    private fun putFirstPage(scope: String, size: Int) {
        redisTemplate.opsForValue().set("news:tech:v1:first-page:$scope:$size", """{"data":[]}""")
    }

    private fun get(key: String): String? = redisTemplate.opsForValue().get(key)

    @Test
    fun `API가 저장한 첫 페이지 키를 실제로 지운다 — 다른 도메인 키는 무사`() {
        putFirstPage("all", 20)
        putFirstPage("2", 20)
        putFirstPage("7", 50)
        redisTemplate.opsForValue().set("ums:users:v1:nickname:1", "\"닉네임\"") // 닉네임 캐시 — 오폭 검증용

        invalidator.clear(listOf(2L, 7L))

        assertNull(get("news:tech:v1:first-page:all:20"))
        assertNull(get("news:tech:v1:first-page:2:20"))
        assertNull(get("news:tech:v1:first-page:7:50"))
        assertEquals("\"닉네임\"", get("ums:users:v1:nickname:1")) // 열거 밖 도메인 키는 유지
    }

    @Test
    fun `열거에 넘기지 않은 카테고리 스코프는 지워지지 않는다 — 활성 카테고리 전체 전달 계약`() {
        putFirstPage("4", 20)

        invalidator.clear(listOf(2L))

        // clear는 (all + 전달된 카테고리)만 열거 — 4는 열거 밖. 호출부(요약 배치)가
        // 활성 카테고리 마스터 전체를 넘겨야 하는 이유가 이 동작
        assertEquals("""{"data":[]}""", get("news:tech:v1:first-page:4:20"))
    }

    @Test
    fun `size 열거 경계 — 1과 MAX_PAGE_SIZE(50)는 지워지고 그 밖은 남는다`() {
        putFirstPage("all", 1)
        putFirstPage("all", 50)
        putFirstPage("all", 51) // API는 size를 1~50으로 보정해 저장하므로 실제로는 생기지 않는 키

        invalidator.clear(emptyList())

        assertNull(get("news:tech:v1:first-page:all:1"))
        assertNull(get("news:tech:v1:first-page:all:50"))
        assertEquals("""{"data":[]}""", get("news:tech:v1:first-page:all:51"))
    }

    @Test
    fun `지울 키가 하나도 없어도 에러 없이 동작한다 — UNLINK는 없는 키를 무시`() {
        assertDoesNotThrow { invalidator.clear(listOf(1L, 2L, 3L)) }
    }
}
