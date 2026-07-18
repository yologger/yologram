package link.yologram.api.v1.domain.tech.article.service

import link.yologram.api.v1.domain.tech.article.entity.TechArticle
import link.yologram.api.v1.domain.tech.article.enums.TechArticleStatus
import link.yologram.api.v1.domain.tech.article.exception.InvalidTechArticleCursorException
import link.yologram.api.v1.domain.tech.article.model.TechArticleCursor
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
    private val service = TechArticleService(techArticleRepository)

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
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), any())).thenReturn(listOf(article(2), last))

        val result = service.getArticlesByCursor(cursor = null, size = 20)

        assertEquals(2, result.data.size)
        assertEquals("제목 2", result.data[0].title)
        assertEquals("요약 2", result.data[0].summary)
        assertEquals(TechArticleCursor.encode(last.publishedAt, last.id), result.nextCursor)
    }

    @Test
    fun `결과가 비면 nextCursor는 null이다`() {
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), any())).thenReturn(emptyList())

        val result = service.getArticlesByCursor(cursor = null, size = 20)

        assertEquals(0, result.data.size)
        assertNull(result.nextCursor)
    }

    @Test
    fun `커서가 디코딩되어 리포지토리로 전달된다`() {
        val publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0)
        val cursor = TechArticleCursor.encode(publishedAt, 42L)
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), any())).thenReturn(emptyList())

        service.getArticlesByCursor(cursor = cursor, size = 20)

        verify(techArticleRepository).findSummarizedArticles(eq(TechArticleCursor(publishedAt, 42L)), eq(20))
    }

    @Test
    fun `잘못된 커서면 INVALID_CURSOR 예외가 발생한다`() {
        assertThrows<InvalidTechArticleCursorException> {
            service.getArticlesByCursor(cursor = "@@@", size = 20)
        }
        verify(techArticleRepository, never()).findSummarizedArticles(anyOrNull(), any())
    }

    @Test
    fun `size는 1~50으로 보정된다`() {
        whenever(techArticleRepository.findSummarizedArticles(anyOrNull(), any())).thenReturn(emptyList())

        service.getArticlesByCursor(cursor = null, size = 999)
        verify(techArticleRepository).findSummarizedArticles(anyOrNull(), eq(TechArticleService.MAX_PAGE_SIZE))

        service.getArticlesByCursor(cursor = null, size = -1)
        verify(techArticleRepository).findSummarizedArticles(anyOrNull(), eq(1))
    }
}
