package link.yologram.worker.domain.tech.article.service

import link.yologram.worker.domain.tech.article.client.CollectedArticle
import link.yologram.worker.domain.tech.article.client.RssFeedClient
import link.yologram.worker.domain.tech.article.entity.TechArticle
import link.yologram.worker.domain.tech.article.entity.TechArticleSource
import link.yologram.worker.domain.tech.article.enums.TechArticleStatus
import link.yologram.worker.domain.tech.article.repository.TechArticleRepository
import link.yologram.worker.domain.tech.article.repository.TechArticleSourceRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals

class TechArticleCollectServiceTest {

    private val techArticleSourceRepository: TechArticleSourceRepository = mock()
    private val techArticleRepository: TechArticleRepository = mock()
    private val rssFeedClient: RssFeedClient = mock()

    private val service = TechArticleCollectService(
        techArticleSourceRepository,
        techArticleRepository,
        rssFeedClient,
    )

    private fun source(id: Long = 1, name: String = "테크 블로그", url: String = "https://tech.example.com/feed") =
        TechArticleSource(id = id, name = name, url = url)

    private fun article(link: String, title: String = "제목") = CollectedArticle(
        title = title,
        link = link,
        publishedAt = LocalDateTime.of(2026, 7, 6, 9, 0),
    )

    @Test
    fun `활성 소스에서 신규 기사만 저장한다`() {
        val src = source()
        whenever(techArticleSourceRepository.findByIsActiveTrue()).thenReturn(listOf(src))
        whenever(rssFeedClient.fetch(src.url)).thenReturn(listOf(article("https://a/1"), article("https://a/2")))
        // https://a/1 은 이미 저장됨
        whenever(techArticleRepository.findExistingLinks(any())).thenReturn(listOf("https://a/1"))
        whenever(techArticleRepository.saveAll(any<List<TechArticle>>())).thenAnswer { it.arguments[0] }

        val result = service.collect()

        assertEquals(1, result.sourceCount)
        assertEquals(1, result.savedCount)
        assertEquals(0, result.failedSourceCount)

        val captor = argumentCaptor<List<TechArticle>>()
        verify(techArticleRepository).saveAll(captor.capture())
        val saved = captor.firstValue.single()
        assertEquals("https://a/2", saved.link)
        assertEquals(src.id, saved.sourceId)
        assertEquals(src.name, saved.sourceName)
        assertEquals(TechArticleStatus.COLLECTED, saved.status)
        assertEquals(0, saved.retryCount)
    }

    @Test
    fun `피드 내 중복 link는 한 건만 저장한다`() {
        val src = source()
        whenever(techArticleSourceRepository.findByIsActiveTrue()).thenReturn(listOf(src))
        whenever(rssFeedClient.fetch(src.url)).thenReturn(listOf(article("https://a/1", "먼저"), article("https://a/1", "나중")))
        whenever(techArticleRepository.findExistingLinks(any())).thenReturn(emptyList())
        whenever(techArticleRepository.saveAll(any<List<TechArticle>>())).thenAnswer { it.arguments[0] }

        val result = service.collect()

        assertEquals(1, result.savedCount)
        val captor = argumentCaptor<List<TechArticle>>()
        verify(techArticleRepository).saveAll(captor.capture())
        assertEquals("먼저", captor.firstValue.single().title)
    }

    @Test
    fun `모든 기사가 이미 저장돼 있으면 저장하지 않는다`() {
        val src = source()
        whenever(techArticleSourceRepository.findByIsActiveTrue()).thenReturn(listOf(src))
        whenever(rssFeedClient.fetch(src.url)).thenReturn(listOf(article("https://a/1")))
        whenever(techArticleRepository.findExistingLinks(any())).thenReturn(listOf("https://a/1"))

        val result = service.collect()

        assertEquals(0, result.savedCount)
        verify(techArticleRepository, never()).saveAll(any<List<TechArticle>>())
    }

    @Test
    fun `피드가 비어 있으면 저장하지 않는다`() {
        val src = source()
        whenever(techArticleSourceRepository.findByIsActiveTrue()).thenReturn(listOf(src))
        whenever(rssFeedClient.fetch(src.url)).thenReturn(emptyList())

        val result = service.collect()

        assertEquals(0, result.savedCount)
        verify(techArticleRepository, never()).findExistingLinks(any())
        verify(techArticleRepository, never()).saveAll(any<List<TechArticle>>())
    }

    @Test
    fun `한 소스가 실패해도 다른 소스 수집은 계속된다`() {
        val failing = source(id = 1, name = "실패 소스", url = "https://fail.example.com/feed")
        val healthy = source(id = 2, name = "정상 소스", url = "https://ok.example.com/feed")
        whenever(techArticleSourceRepository.findByIsActiveTrue()).thenReturn(listOf(failing, healthy))
        whenever(rssFeedClient.fetch(failing.url)).doThrow(RuntimeException("connection timeout"))
        whenever(rssFeedClient.fetch(healthy.url)).thenReturn(listOf(article("https://b/1")))
        whenever(techArticleRepository.findExistingLinks(any())).thenReturn(emptyList())
        whenever(techArticleRepository.saveAll(any<List<TechArticle>>())).thenAnswer { it.arguments[0] }

        val result = service.collect()

        assertEquals(2, result.sourceCount)
        assertEquals(1, result.savedCount)
        assertEquals(1, result.failedSourceCount)
    }

    @Test
    fun `활성 소스가 없으면 아무것도 하지 않는다`() {
        whenever(techArticleSourceRepository.findByIsActiveTrue()).thenReturn(emptyList())

        val result = service.collect()

        assertEquals(0, result.sourceCount)
        assertEquals(0, result.savedCount)
        assertEquals(0, result.failedSourceCount)
    }
}
