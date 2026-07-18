package link.yologram.api.v1.domain.tech.article.service

import link.yologram.api.v1.domain.tech.article.entity.TechArticle
import link.yologram.api.v1.domain.tech.article.enums.TechArticleStatus
import link.yologram.api.v1.domain.tech.article.exception.InvalidTechArticleCursorException
import link.yologram.api.v1.domain.tech.article.model.TechArticleCursor
import link.yologram.api.v1.domain.tech.article.entity.TechArticleCategoryMapping
import link.yologram.api.v1.domain.tech.article.repository.TechArticleCategoryMappingRepository
import link.yologram.api.v1.domain.tech.article.repository.TechArticleRepository
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

class TechArticleServiceTest {

    private val techArticleRepository: TechArticleRepository = mock()
    private val mappingRepository: TechArticleCategoryMappingRepository = mock()
    private val service = TechArticleService(techArticleRepository, mappingRepository)

    init {
        whenever(mappingRepository.findByArticleIdIn(any())).thenReturn(emptyList())
    }

    private fun article(id: Long, publishedAt: LocalDateTime = LocalDateTime.of(2026, 7, 18, 9, 0)) = TechArticle(
        id = id,
        sourceId = 1,
        title = "제목 $id",
        link = "https://a/$id",
        summary = "요약 $id",
        sourceName = "테크 블로그",
        publishedAt = publishedAt,
        status = TechArticleStatus.SUMMARIZED,
    )

    @Test
    fun `아티클 목록과 nextCursor를 반환한다`() {
        val last = article(1, LocalDateTime.of(2026, 7, 17, 9, 0))
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), anyOrNull(), any())).thenReturn(listOf(article(2), last))

        val result = service.getArticlesByCursor(category = null, cursor = null, size = 20)

        assertEquals(2, result.data.size)
        assertEquals("제목 2", result.data[0].title)
        assertEquals("요약 2", result.data[0].summary)
        assertEquals(TechArticleCursor.encode(last.publishedAt, last.id), result.nextCursor)
    }

    @Test
    fun `카테고리가 배치 조회되어 응답에 매핑된다`() {
        val a1 = article(1)
        val a2 = article(2)
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), anyOrNull(), any())).thenReturn(listOf(a1, a2))
        whenever(mappingRepository.findByArticleIdIn(listOf(1L, 2L))).thenReturn(
            listOf(
                TechArticleCategoryMapping(id = 1, articleId = 1L, category = "Backend"),
                TechArticleCategoryMapping(id = 2, articleId = 1L, category = "Cloud"),
            )
        )

        val result = service.getArticlesByCursor(category = null, cursor = null, size = 20)

        assertEquals(listOf("Backend", "Cloud"), result.data[0].categories)
        assertEquals(emptyList(), result.data[1].categories) // 매핑 없는 글은 빈 목록
    }

    @Test
    fun `category 필터가 리포지토리로 전달된다`() {
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        service.getArticlesByCursor(category = "Backend", cursor = null, size = 20)

        verify(techArticleRepository).findSummarizedArticles(eq("Backend"), anyOrNull(), eq(20))
    }

    @Test
    fun `결과가 비면 nextCursor는 null이다`() {
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        val result = service.getArticlesByCursor(category = null, cursor = null, size = 20)

        assertEquals(0, result.data.size)
        assertNull(result.nextCursor)
    }

    @Test
    fun `커서가 디코딩되어 리포지토리로 전달된다`() {
        val publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0)
        val cursor = TechArticleCursor.encode(publishedAt, 42L)
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        service.getArticlesByCursor(category = null, cursor = cursor, size = 20)

        verify(techArticleRepository).findSummarizedArticles(anyOrNull(), eq(TechArticleCursor(publishedAt, 42L)), eq(20))
    }

    @Test
    fun `잘못된 커서면 INVALID_CURSOR 예외가 발생한다`() {
        assertThrows<InvalidTechArticleCursorException> {
            service.getArticlesByCursor(category = null, cursor = "@@@", size = 20)
        }
        verify(techArticleRepository, never()).findSummarizedArticles(anyOrNull(), anyOrNull(), any())
    }

    @Test
    fun `size는 1~50으로 보정된다`() {
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        service.getArticlesByCursor(category = null, cursor = null, size = 999)
        verify(techArticleRepository).findSummarizedArticles(anyOrNull(), anyOrNull(), eq(TechArticleService.MAX_PAGE_SIZE))

        service.getArticlesByCursor(category = null, cursor = null, size = -1)
        verify(techArticleRepository).findSummarizedArticles(anyOrNull(), anyOrNull(), eq(1))
    }
}
