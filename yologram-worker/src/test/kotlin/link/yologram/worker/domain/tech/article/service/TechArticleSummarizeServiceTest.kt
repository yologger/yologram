package link.yologram.worker.domain.tech.article.service

import link.yologram.worker.domain.tech.article.client.ArticleContentCrawler
import link.yologram.worker.domain.tech.article.entity.TechArticle
import link.yologram.worker.domain.tech.article.enums.TechArticleStatus
import link.yologram.worker.domain.tech.article.entity.TechArticleCategoryMapping
import link.yologram.worker.domain.tech.article.repository.TechArticleCategoryMappingRepository
import link.yologram.worker.domain.tech.article.repository.TechArticleRepository
import link.yologram.worker.global.discord.DiscordNotifier
import link.yologram.worker.global.llm.LlmClient
import link.yologram.worker.global.llm.LlmCompletion
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TechArticleSummarizeServiceTest {

    private val techArticleRepository: TechArticleRepository = mock()
    private val techArticleCategoryMappingRepository: TechArticleCategoryMappingRepository = mock()
    private val articleContentCrawler: ArticleContentCrawler = mock()
    private val llmClient: LlmClient = mock {
        on { available } doReturn true
    }
    private val notifier: DiscordNotifier = mock()

    // ifAvailable 호출 시 mock notifier를 넘겨주는 provider (빈 존재 상황 재현)
    private val notifierProvider: ObjectProvider<DiscordNotifier> = mock()

    init {
        @Suppress("UNCHECKED_CAST")
        org.mockito.kotlin.doAnswer { (it.arguments[0] as java.util.function.Consumer<DiscordNotifier>).accept(notifier) }
            .whenever(notifierProvider).ifAvailable(any())
    }

    private val service = TechArticleSummarizeService(
        techArticleRepository,
        techArticleCategoryMappingRepository,
        articleContentCrawler,
        llmClient,
        notifierProvider,
    )

    private fun article(id: Long = 1, link: String = "https://a/$id", retryCount: Int = 0) = TechArticle(
        id = id,
        sourceId = 1,
        title = "제목 $id",
        link = link,
        sourceName = "테크 블로그",
        publishedAt = LocalDateTime.of(2026, 7, 6, 9, 0),
        retryCount = retryCount,
    )

    private fun stubTargets(vararg articles: TechArticle) {
        whenever(
            techArticleRepository.findByStatusAndRetryCountLessThan(
                eq(TechArticleStatus.COLLECTED),
                eq(TechArticleSummarizeService.MAX_RETRY),
                any(),
            )
        ).thenReturn(articles.toList())
    }

    @Test
    fun `크롤링한 본문을 요약해 SUMMARIZED로 전환한다`() {
        val target = article()
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).thenReturn("본문 텍스트")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "한국어 요약"))

        val result = service.summarize()

        assertEquals(1, result.targetCount)
        assertEquals(1, result.summarizedCount)
        assertEquals(0, result.failedCount)
        assertEquals("한국어 요약", target.summary)
        assertEquals(TechArticleStatus.SUMMARIZED, target.status)
        verify(techArticleRepository).save(target)
        // 카테고리 마커 없는 출력 → [기타] 폴백 매핑
        verify(techArticleCategoryMappingRepository).deleteByArticleIdBulk(target.id)
        verify(techArticleCategoryMappingRepository).saveAll(org.mockito.kotlin.check<List<TechArticleCategoryMapping>> {
            assertEquals(listOf("기타"), it.map { m -> m.category })
        })
    }

    @Test
    fun `LLM 출력의 카테고리 섹션을 분리해 매핑을 저장하고 summary에서 제거한다`() {
        val target = article()
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).thenReturn(
            LlmCompletion("gemini", "**📌 한 줄 요약**\n코루틴 해설.\n\n**🏷️ 카테고리**\nBackend, DevOps")
        )

        service.summarize()

        assertEquals("**📌 한 줄 요약**\n코루틴 해설.", target.summary)
        verify(techArticleCategoryMappingRepository).deleteByArticleIdBulk(target.id)
        verify(techArticleCategoryMappingRepository).saveAll(org.mockito.kotlin.check<List<TechArticleCategoryMapping>> {
            assertEquals(listOf("Backend", "DevOps"), it.map { m -> m.category })
            assertEquals(listOf(target.id, target.id), it.map { m -> m.articleId })
        })
    }

    @Test
    fun `프롬프트에 제목과 본문이 포함된다`() {
        val target = article()
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).thenReturn("본문 텍스트")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "요약"))

        service.summarize()

        verify(llmClient).complete(org.mockito.kotlin.check {
            kotlin.test.assertTrue(it.contains("제목 1"))
            kotlin.test.assertTrue(it.contains("https://a/1"))
            kotlin.test.assertTrue(it.contains("본문 텍스트"))
            kotlin.test.assertTrue(it.contains("출력 형식"))
        })
    }

    @Test
    fun `크롤링 실패 시 retryCount가 증가하고 COLLECTED로 남는다`() {
        val target = article()
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).doThrow(RuntimeException("403 Forbidden"))

        val result = service.summarize()

        assertEquals(1, result.failedCount)
        assertEquals(1, target.retryCount)
        assertEquals(TechArticleStatus.COLLECTED, target.status)
        assertNull(target.summary)
        verify(llmClient, never()).complete(any())
        verify(techArticleRepository).save(target)
        verify(techArticleCategoryMappingRepository, never()).saveAll(any<List<TechArticleCategoryMapping>>())
    }

    @Test
    fun `재시도 한도에 도달하면 FAILED로 전환한다`() {
        val target = article(retryCount = TechArticleSummarizeService.MAX_RETRY - 1)
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).doThrow(RuntimeException("403 Forbidden"))

        service.summarize()

        assertEquals(TechArticleSummarizeService.MAX_RETRY, target.retryCount)
        assertEquals(TechArticleStatus.FAILED, target.status)
    }

    @Test
    fun `FAILED 확정 시 Discord로 경고를 발송한다`() {
        val target = article(retryCount = TechArticleSummarizeService.MAX_RETRY - 1)
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).doThrow(RuntimeException("403 Forbidden"))

        service.summarize()

        verify(notifier).send(eq(DiscordNotifier.CHANNEL_TECH), org.mockito.kotlin.check {
            kotlin.test.assertTrue(it.contains("요약 최종 실패"))
            kotlin.test.assertTrue(it.contains(target.title))
            kotlin.test.assertTrue(it.contains("403 Forbidden"))
        })
    }

    @Test
    fun `재시도가 남아 있는 실패는 Discord 경고를 발송하지 않는다`() {
        val target = article(retryCount = 0)
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).doThrow(RuntimeException("timeout"))

        service.summarize()

        verify(notifierProvider, never()).ifAvailable(any())
    }

    @Test
    fun `LLM 실패도 재시도 대상으로 처리한다`() {
        val target = article()
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).doThrow(IllegalStateException("모든 LLM 제공자 호출 실패"))

        val result = service.summarize()

        assertEquals(1, result.failedCount)
        assertEquals(1, target.retryCount)
        assertEquals(TechArticleStatus.COLLECTED, target.status)
    }

    @Test
    fun `한 건이 실패해도 나머지는 계속 처리한다`() {
        val failing = article(id = 1)
        val healthy = article(id = 2)
        stubTargets(failing, healthy)
        whenever(articleContentCrawler.fetch(failing.link)).doThrow(RuntimeException("timeout"))
        whenever(articleContentCrawler.fetch(healthy.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("groq", "요약"))

        val result = service.summarize()

        assertEquals(2, result.targetCount)
        assertEquals(1, result.summarizedCount)
        assertEquals(1, result.failedCount)
        assertEquals(TechArticleStatus.SUMMARIZED, healthy.status)
    }

    @Test
    fun `요약 성공한 글은 Discord embed로 발송한다`() {
        val target = article()
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "한국어 요약"))

        service.summarize()

        verify(notifier).sendEmbed(
            channel = DiscordNotifier.CHANNEL_TECH,
            title = target.title,
            url = target.link,
            description = "한국어 요약",
            sourceName = target.sourceName,
        )
    }

    @Test
    fun `요약 실패한 글은 Discord 발송을 하지 않는다`() {
        val target = article()
        stubTargets(target)
        whenever(articleContentCrawler.fetch(target.link)).doThrow(RuntimeException("403"))

        service.summarize()

        verify(notifierProvider, never()).ifAvailable(any())
    }

    @Test
    fun `요약 대상이 없으면 아무것도 하지 않는다`() {
        stubTargets()

        val result = service.summarize()

        assertEquals(0, result.targetCount)
        verify(techArticleRepository, never()).save(any())
    }

    @Test
    fun `LLM 미구성이면 조회 없이 스킵한다`() {
        whenever(llmClient.available).thenReturn(false)

        val result = service.summarize()

        assertEquals(0, result.targetCount)
        verify(techArticleRepository, never()).findByStatusAndRetryCountLessThan(any(), any(), any())
    }
}
