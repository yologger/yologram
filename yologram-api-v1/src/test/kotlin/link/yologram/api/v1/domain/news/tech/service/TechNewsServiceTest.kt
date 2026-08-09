package link.yologram.api.v1.domain.news.tech.service

import link.yologram.api.v1.domain.news.tech.entity.TechNews
import link.yologram.api.v1.domain.news.tech.enums.TechNewsStatus
import link.yologram.api.v1.domain.news.tech.exception.InvalidTechNewsCursorException
import link.yologram.api.v1.domain.news.tech.model.TechNewsCursor
import link.yologram.api.v1.domain.news.tech.entity.TechNewsCategoryMapping
import link.yologram.api.v1.domain.news.tech.repository.TechNewsCategoryMappingRepository
import link.yologram.api.v1.domain.news.tech.repository.TechNewsRepository
import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import link.yologram.api.v1.infra.cache.Cache
import link.yologram.api.v1.infra.cache.CacheService
import link.yologram.api.v1.infra.cache.TechNewsFirstPageCache
import link.yologram.api.v1.infra.client.cms.CmsApiClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TechNewsServiceTest {

    private val techNewsRepository: TechNewsRepository = mock()
    private val mappingRepository: TechNewsCategoryMappingRepository = mock()
    private val cmsApiClient: CmsApiClient = mock()
    private val cacheService: CacheService = mock() // 기본 스텁이 null(전체 미스)이라 기존 테스트는 DB 경로 그대로
    private val service = TechNewsService(techNewsRepository, mappingRepository, cmsApiClient, TechNewsFirstPageCache(cacheService))

    init {
        whenever(mappingRepository.findByNewsIdIn(any())).thenReturn(emptyList())
        whenever(cmsApiClient.findCategoryNames(any())).thenReturn(emptyMap())
    }

    private fun news(id: Long, publishedAt: LocalDateTime = LocalDateTime.of(2026, 7, 18, 9, 0)) = TechNews(
        id = id,
        sourceId = 1,
        title = "제목 $id",
        link = "https://a/$id",
        summary = "요약 $id",
        sourceName = "테크 블로그",
        publishedAt = publishedAt,
        status = TechNewsStatus.SUMMARIZED,
    )

    @Test
    fun `뉴스 목록과 nextCursor를 반환한다`() {
        val last = news(1, LocalDateTime.of(2026, 7, 17, 9, 0))
        whenever(techNewsRepository.findSummarizedNews(anyOrNull(), anyOrNull(), any())).thenReturn(listOf(news(2), last))

        val result = service.getNewsByCursor(categoryId = null, cursor = null, size = 20)

        assertEquals(2, result.data.size)
        assertEquals("제목 2", result.data[0].title)
        assertEquals("요약 2", result.data[0].summary)
        assertEquals(TechNewsCursor.encode(last.publishedAt, last.id), result.nextCursor)
    }

    @Test
    fun `카테고리가 배치 조회되어 응답에 매핑된다`() {
        val a1 = news(1)
        val a2 = news(2)
        whenever(techNewsRepository.findSummarizedNews(anyOrNull(), anyOrNull(), any())).thenReturn(listOf(a1, a2))
        whenever(mappingRepository.findByNewsIdIn(listOf(1L, 2L))).thenReturn(
            listOf(
                TechNewsCategoryMapping(id = 1, newsId = 1L, categoryId = 2L),
                TechNewsCategoryMapping(id = 2, newsId = 1L, categoryId = 5L),
            )
        )
        whenever(cmsApiClient.findCategoryNames(listOf(2L, 5L))).thenReturn(
            mapOf(2L to "Backend", 5L to "Cloud")
        )

        val result = service.getNewsByCursor(categoryId = null, cursor = null, size = 20)

        assertEquals(listOf("Backend", "Cloud"), result.data[0].categories)
        assertEquals(emptyList(), result.data[1].categories) // 매핑 없는 글은 빈 목록
    }

    @Test
    fun `categoryId 필터가 리포지토리로 전달된다`() {
        whenever(techNewsRepository.findSummarizedNews(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        service.getNewsByCursor(categoryId = 2L, cursor = null, size = 20)

        verify(techNewsRepository).findSummarizedNews(eq(2L), anyOrNull(), eq(20))
    }

    @Test
    fun `결과가 비면 nextCursor는 null이다`() {
        whenever(techNewsRepository.findSummarizedNews(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        val result = service.getNewsByCursor(categoryId = null, cursor = null, size = 20)

        assertEquals(0, result.data.size)
        assertNull(result.nextCursor)
    }

    @Test
    fun `커서가 디코딩되어 리포지토리로 전달된다`() {
        val publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0)
        val cursor = TechNewsCursor.encode(publishedAt, 42L)
        whenever(techNewsRepository.findSummarizedNews(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        service.getNewsByCursor(categoryId = null, cursor = cursor, size = 20)

        verify(techNewsRepository).findSummarizedNews(anyOrNull(), eq(TechNewsCursor(publishedAt, 42L)), eq(20))
    }

    @Test
    fun `잘못된 커서면 INVALID_CURSOR 예외가 발생한다`() {
        assertThrows<InvalidTechNewsCursorException> {
            service.getNewsByCursor(categoryId = null, cursor = "@@@", size = 20)
        }
        verify(techNewsRepository, never()).findSummarizedNews(anyOrNull(), anyOrNull(), any())
    }

    @Test
    fun `size는 1~50으로 보정된다`() {
        whenever(techNewsRepository.findSummarizedNews(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        service.getNewsByCursor(categoryId = null, cursor = null, size = 999)
        verify(techNewsRepository).findSummarizedNews(anyOrNull(), anyOrNull(), eq(TechNewsService.MAX_PAGE_SIZE))

        service.getNewsByCursor(categoryId = null, cursor = null, size = -1)
        verify(techNewsRepository).findSummarizedNews(anyOrNull(), anyOrNull(), eq(1))
    }

    @Test
    fun `cursor가 있으면 캐시를 경유하지 않는다`() {
        val cursor = TechNewsCursor.encode(LocalDateTime.of(2026, 7, 18, 9, 0), 42L)
        whenever(techNewsRepository.findSummarizedNews(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        service.getNewsByCursor(categoryId = null, cursor = cursor, size = 20)

        verifyNoInteractions(cacheService)
    }

    @Test
    fun `cursor가 없으면 캐시를 경유하고 미스 시 결과를 캐시에 저장한다`() {
        whenever(techNewsRepository.findSummarizedNews(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        service.getNewsByCursor(categoryId = null, cursor = null, size = 20)

        // categoryId null(all) — coerce된 size가 키에 반영
        verify(cacheService).set(
            argThat<Cache<ApiEnvelopCursorPage<TechNewsResponse>>> { key == Cache.techNewsFirstPage(null, 20).key },
            any(),
        )
    }

    @Test
    fun `cursor가 없고 캐시 히트면 DB를 조회하지 않는다`() {
        val cached = ApiEnvelopCursorPage<TechNewsResponse>(data = emptyList(), nextCursor = null)
        whenever(cacheService.getOrNull(any<Cache<Any>>())).thenAnswer { invocation ->
            val key = invocation.getArgument<Cache<*>>(0).key
            if (key == Cache.techNewsFirstPage(null, 20).key) cached else null
        }

        val result = service.getNewsByCursor(categoryId = null, cursor = null, size = 20)

        assertEquals(cached, result)
        verify(techNewsRepository, never()).findSummarizedNews(anyOrNull(), anyOrNull(), any())
    }
}
