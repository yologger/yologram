package link.yologram.worker.infra.cache

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TechNewsFirstPageCacheInvalidatorTest {

    private val stringRedisTemplate: StringRedisTemplate = mock()

    private val invalidator = TechNewsFirstPageCacheInvalidator(stringRedisTemplate)

    @Test
    fun `all 스코프와 카테고리 스코프의 전 size 키를 한 번의 UNLINK로 지운다`() {
        invalidator.clear(listOf(2L, 4L))

        val captor = argumentCaptor<Collection<String>>()
        verify(stringRedisTemplate).unlink(captor.capture())

        val keys = captor.firstValue
        // (all + 2 + 4) × size 1~50 = 150개 전수 열거
        assertEquals(3 * TechNewsFirstPageCacheInvalidator.MAX_PAGE_SIZE, keys.size)
        assertTrue("news:tech:v1:first-page:all:1" in keys)
        assertTrue("news:tech:v1:first-page:all:50" in keys)
        assertTrue("news:tech:v1:first-page:2:20" in keys)
        assertTrue("news:tech:v1:first-page:4:50" in keys)
        // 열거에 없는 스코프는 포함되지 않음 (닉네임 캐시 등 오폭 없음은 키 prefix로 보장)
        assertTrue(keys.none { it.contains(":7:") })
    }

    @Test
    fun `카테고리가 비어 있으면 all 스코프 키만 지운다`() {
        invalidator.clear(emptyList())

        val captor = argumentCaptor<Collection<String>>()
        verify(stringRedisTemplate).unlink(captor.capture())
        assertEquals(TechNewsFirstPageCacheInvalidator.MAX_PAGE_SIZE, captor.firstValue.size)
        assertTrue(captor.firstValue.all { it.startsWith("news:tech:v1:first-page:all:") })
    }

    @Test
    fun `중복 카테고리는 한 번만 열거한다`() {
        invalidator.clear(listOf(2L, 2L, 2L))

        val captor = argumentCaptor<Collection<String>>()
        verify(stringRedisTemplate).unlink(captor.capture())
        // (all + 2) × 50 — 중복 열거 없음
        assertEquals(2 * TechNewsFirstPageCacheInvalidator.MAX_PAGE_SIZE, captor.firstValue.size)
    }

    @Test
    fun `UNLINK 실패(Redis 다운)는 예외를 전파하지 않고 삼킨다`() {
        whenever(stringRedisTemplate.unlink(any<Collection<String>>()))
            .doThrow(RedisConnectionFailureException("connection refused"))

        // 무효화는 부가 기능 — 실패해도 호출자(요약 배치)에 영향 없음 (API 캐시 TTL 3분이 보험)
        assertDoesNotThrow { invalidator.clear(listOf(2L)) }
    }
}
