package link.yologram.api.v1.domain.tech.news.service

import link.yologram.api.v1.domain.tech.news.entity.TechNews
import link.yologram.api.v1.domain.tech.news.enums.TechNewsStatus
import link.yologram.api.v1.domain.tech.news.exception.InvalidTechNewsCursorException
import link.yologram.api.v1.domain.tech.news.model.TechNewsCursor
import link.yologram.api.v1.domain.tech.news.entity.TechNewsCategoryMapping
import link.yologram.api.v1.domain.tech.news.repository.TechNewsCategoryMappingRepository
import link.yologram.api.v1.domain.tech.news.repository.TechNewsRepository
import link.yologram.api.v1.domain.tech.category.entity.TechCategory
import link.yologram.api.v1.domain.tech.category.repository.TechCategoryRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TechNewsServiceTest {

    private val techNewsRepository: TechNewsRepository = mock()
    private val mappingRepository: TechNewsCategoryMappingRepository = mock()
    private val categoryRepository: TechCategoryRepository = mock()
    private val service = TechNewsService(techNewsRepository, mappingRepository, categoryRepository)

    init {
        whenever(mappingRepository.findByNewsIdIn(any())).thenReturn(emptyList())
        whenever(categoryRepository.findAllById(any())).thenReturn(emptyList())
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
        whenever(categoryRepository.findAllById(any())).thenReturn(
            listOf(
                TechCategory(id = 2, name = "Backend", sortOrder = 2),
                TechCategory(id = 5, name = "Cloud", sortOrder = 5),
            )
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
}
