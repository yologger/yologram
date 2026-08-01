package link.yologram.worker.domain.news.tech.service

import link.yologram.worker.domain.news.tech.client.CollectedNews
import link.yologram.worker.domain.news.tech.client.RssFeedClient
import link.yologram.worker.domain.news.tech.entity.TechNews
import link.yologram.worker.domain.news.tech.entity.TechNewsSource
import link.yologram.worker.domain.news.tech.enums.TechNewsStatus
import link.yologram.worker.domain.news.tech.repository.TechNewsRepository
import link.yologram.worker.domain.news.tech.repository.TechNewsSourceRepository
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

class TechNewsCollectServiceTest {

    private val techNewsSourceRepository: TechNewsSourceRepository = mock()
    private val techNewsRepository: TechNewsRepository = mock()
    private val rssFeedClient: RssFeedClient = mock()

    private val service = TechNewsCollectService(
        techNewsSourceRepository,
        techNewsRepository,
        rssFeedClient,
    )

    private fun source(id: Long = 1, name: String = "테크 블로그", url: String = "https://tech.example.com/feed") =
        TechNewsSource(id = id, name = name, url = url)

    private fun news(link: String, title: String = "제목") = CollectedNews(
        title = title,
        link = link,
        publishedAt = LocalDateTime.of(2026, 7, 6, 9, 0),
    )

    @Test
    fun `활성 소스에서 신규 기사만 저장한다`() {
        val src = source()
        whenever(techNewsSourceRepository.findByIsActiveTrue()).thenReturn(listOf(src))
        whenever(rssFeedClient.fetch(src.url)).thenReturn(listOf(news("https://a/1"), news("https://a/2")))
        // https://a/1 은 이미 저장됨
        whenever(techNewsRepository.findExistingLinks(any())).thenReturn(listOf("https://a/1"))
        whenever(techNewsRepository.saveAll(any<List<TechNews>>())).thenAnswer { it.arguments[0] }

        val result = service.collect()

        assertEquals(1, result.sourceCount)
        assertEquals(1, result.savedCount)
        assertEquals(0, result.failedSourceCount)

        val captor = argumentCaptor<List<TechNews>>()
        verify(techNewsRepository).saveAll(captor.capture())
        val saved = captor.firstValue.single()
        assertEquals("https://a/2", saved.link)
        assertEquals(src.id, saved.sourceId)
        assertEquals(src.name, saved.sourceName)
        assertEquals(TechNewsStatus.COLLECTED, saved.status)
        assertEquals(0, saved.retryCount)
    }

    @Test
    fun `피드 내 중복 link는 한 건만 저장한다`() {
        val src = source()
        whenever(techNewsSourceRepository.findByIsActiveTrue()).thenReturn(listOf(src))
        whenever(rssFeedClient.fetch(src.url)).thenReturn(listOf(news("https://a/1", "먼저"), news("https://a/1", "나중")))
        whenever(techNewsRepository.findExistingLinks(any())).thenReturn(emptyList())
        whenever(techNewsRepository.saveAll(any<List<TechNews>>())).thenAnswer { it.arguments[0] }

        val result = service.collect()

        assertEquals(1, result.savedCount)
        val captor = argumentCaptor<List<TechNews>>()
        verify(techNewsRepository).saveAll(captor.capture())
        assertEquals("먼저", captor.firstValue.single().title)
    }

    @Test
    fun `모든 기사가 이미 저장돼 있으면 저장하지 않는다`() {
        val src = source()
        whenever(techNewsSourceRepository.findByIsActiveTrue()).thenReturn(listOf(src))
        whenever(rssFeedClient.fetch(src.url)).thenReturn(listOf(news("https://a/1")))
        whenever(techNewsRepository.findExistingLinks(any())).thenReturn(listOf("https://a/1"))

        val result = service.collect()

        assertEquals(0, result.savedCount)
        verify(techNewsRepository, never()).saveAll(any<List<TechNews>>())
    }

    @Test
    fun `피드가 비어 있으면 저장하지 않는다`() {
        val src = source()
        whenever(techNewsSourceRepository.findByIsActiveTrue()).thenReturn(listOf(src))
        whenever(rssFeedClient.fetch(src.url)).thenReturn(emptyList())

        val result = service.collect()

        assertEquals(0, result.savedCount)
        verify(techNewsRepository, never()).findExistingLinks(any())
        verify(techNewsRepository, never()).saveAll(any<List<TechNews>>())
    }

    @Test
    fun `한 소스가 실패해도 다른 소스 수집은 계속된다`() {
        val failing = source(id = 1, name = "실패 소스", url = "https://fail.example.com/feed")
        val healthy = source(id = 2, name = "정상 소스", url = "https://ok.example.com/feed")
        whenever(techNewsSourceRepository.findByIsActiveTrue()).thenReturn(listOf(failing, healthy))
        whenever(rssFeedClient.fetch(failing.url)).doThrow(RuntimeException("connection timeout"))
        whenever(rssFeedClient.fetch(healthy.url)).thenReturn(listOf(news("https://b/1")))
        whenever(techNewsRepository.findExistingLinks(any())).thenReturn(emptyList())
        whenever(techNewsRepository.saveAll(any<List<TechNews>>())).thenAnswer { it.arguments[0] }

        val result = service.collect()

        assertEquals(2, result.sourceCount)
        assertEquals(1, result.savedCount)
        assertEquals(1, result.failedSourceCount)
    }

    @Test
    fun `활성 소스가 없으면 아무것도 하지 않는다`() {
        whenever(techNewsSourceRepository.findByIsActiveTrue()).thenReturn(emptyList())

        val result = service.collect()

        assertEquals(0, result.sourceCount)
        assertEquals(0, result.savedCount)
        assertEquals(0, result.failedSourceCount)
    }
}
