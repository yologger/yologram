package link.yologram.api.v1.infra.cache

import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TechNewsFirstPageCacheTest {

    @Mock
    lateinit var cacheService: CacheService

    lateinit var firstPageCache: TechNewsFirstPageCache

    @BeforeEach
    fun setUp() {
        firstPageCache = TechNewsFirstPageCache(cacheService)
    }

    private fun page(vararg titles: String) = ApiEnvelopCursorPage(
        data = titles.mapIndexed { index, title ->
            TechNewsResponse(
                id = index + 1L,
                title = title,
                summary = "요약 $title",
                link = "https://a/${index + 1}",
                sourceName = "테크 블로그",
                categories = emptyList(),
                publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0),
            )
        },
        nextCursor = null,
    )

    private fun pageKey(categoryId: Long?, size: Int) = Cache.techNewsFirstPage(categoryId, size).key

    private fun stubRedis(pages: Map<String, ApiEnvelopCursorPage<TechNewsResponse>> = emptyMap()) {
        whenever(cacheService.getOrNull(any<Cache<Any>>())).thenAnswer { invocation ->
            pages[invocation.getArgument<Cache<*>>(0).key]
        }
    }

    @Test
    fun `캐시 히트 시 loader를 호출하지 않는다`() {
        val cached = page("캐시된 뉴스")
        stubRedis(pages = mapOf(pageKey(1L, 20) to cached))

        val result = firstPageCache.get(categoryId = 1L, size = 20) { error("loader must not be called") }

        assertEquals(cached, result)
        verify(cacheService, never()).set(any<Cache<Any>>(), any())
    }

    @Test
    fun `미스 시 loader를 호출하고 결과를 캐시에 저장한다`() {
        stubRedis()
        val loaded = page("새 뉴스")

        val result = firstPageCache.get(categoryId = 7L, size = 30) { loaded }

        assertEquals(loaded, result)
        // 키 스킴 고정 검증: news:tech:v1:first-page:{categoryId|all}:{size} — worker UNLINK 열거와의 계약
        assertEquals("news:tech:v1:first-page:7:30", pageKey(7L, 30))
        verify(cacheService).set(
            argThat<Cache<ApiEnvelopCursorPage<TechNewsResponse>>> { key == "news:tech:v1:first-page:7:30" },
            any(),
        )
    }

    @Test
    fun `categoryId가 없으면 all 스코프 키를 쓴다`() {
        stubRedis()

        firstPageCache.get(categoryId = null, size = 20) { page("전체 피드") }

        assertEquals("news:tech:v1:first-page:all:20", pageKey(null, 20))
        verify(cacheService).set(
            argThat<Cache<ApiEnvelopCursorPage<TechNewsResponse>>> { key == "news:tech:v1:first-page:all:20" },
            any(),
        )
    }

    @Test
    fun `Redis 장애(전체 미스) 시 loader(DB)로 폴백해 기능 무손상이다`() {
        // RedisCacheService는 장애 시 getOrNull이 null — 미스와 동일 경로로 DB 폴백
        stubRedis()
        val loaded = page("폴백 뉴스")

        val result = firstPageCache.get(categoryId = null, size = 20) { loaded }

        assertEquals(loaded, result)
    }

    @Test
    fun `TTL은 3분이다 — 삭제 누락·레이스 부활 대비 보험 계약`() {
        assertEquals(Duration.ofMinutes(3), Cache.techNewsFirstPage(null, 20).duration)
    }
}
