package link.yologram.worker.domain.news.tech.service

import link.yologram.worker.domain.news.tech.client.NewsContentCrawler
import link.yologram.worker.domain.news.tech.entity.TechNews
import link.yologram.worker.domain.news.tech.enums.TechNewsStatus
import link.yologram.worker.domain.news.tech.entity.TechNewsCategoryMapping
import link.yologram.worker.domain.news.tech.repository.TechNewsCategoryMappingRepository
import link.yologram.worker.domain.news.tech.repository.TechNewsRepository
import link.yologram.worker.domain.search.tech.service.TechNewsIndexService
import org.springframework.beans.factory.ObjectProvider
import link.yologram.worker.global.discord.DiscordNotifier
import link.yologram.worker.infra.cache.TechNewsFirstPageCacheInvalidator
import link.yologram.worker.infra.client.cms.CmsApiClient
import link.yologram.worker.infra.client.cms.TechCategory
import link.yologram.worker.global.llm.LlmClient
import link.yologram.worker.global.llm.LlmCompletion
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.support.TransactionOperations
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TechNewsSummarizeServiceTest {

    private val techNewsRepository: TechNewsRepository = mock()
    private val techNewsCategoryMappingRepository: TechNewsCategoryMappingRepository = mock()
    private val cmsApiClient: CmsApiClient = mock {
        on { findActiveCategories() } doReturn listOf(
            TechCategory(id = 2, name = "Backend", sortOrder = 2),
            TechCategory(id = 4, name = "DevOps", sortOrder = 4),
            TechCategory(id = 7, name = "기타", sortOrder = 7),
        )
    }
    private val newsContentCrawler: NewsContentCrawler = mock()
    private val llmClient: LlmClient = mock {
        on { available } doReturn true
    }
    private val notifier: DiscordNotifier = mock()

    // ifAvailable 호출 시 mock notifier를 넘겨주는 provider (빈 존재 상황 재현)
    private val notifierProvider: ObjectProvider<DiscordNotifier> = mock()

    // 콜백을 즉시 실행하는 트랜잭션 목 (실제 트랜잭션 경계는 통합 환경에서 검증)
    private val transactionOperations: TransactionOperations = TransactionOperations.withoutTransaction()

    private val cacheInvalidator: TechNewsFirstPageCacheInvalidator = mock()

    private val newsIndexService: TechNewsIndexService = mock()

    /** 검색이 꺼진 환경에서는 빈이 없다 — 기본은 있는 상태로 두고 없는 경우를 따로 검증한다 */
    private val newsIndexServiceProvider: ObjectProvider<TechNewsIndexService> = mock {
        on { ifAvailable } doReturn newsIndexService
    }

    init {
        @Suppress("UNCHECKED_CAST")
        org.mockito.kotlin.doAnswer { (it.arguments[0] as java.util.function.Consumer<DiscordNotifier>).accept(notifier) }
            .whenever(notifierProvider).ifAvailable(any())
    }

    private val service = TechNewsSummarizeService(
        techNewsRepository,
        techNewsCategoryMappingRepository,
        cmsApiClient,
        newsContentCrawler,
        llmClient,
        notifierProvider,
        transactionOperations,
        cacheInvalidator,
        newsIndexServiceProvider,
    )

    private fun news(id: Long = 1, link: String = "https://a/$id", retryCount: Int = 0) = TechNews(
        id = id,
        sourceId = 1,
        title = "제목 $id",
        link = link,
        sourceName = "테크 블로그",
        publishedAt = LocalDateTime.of(2026, 7, 6, 9, 0),
        retryCount = retryCount,
    )

    private fun stubTargets(vararg newsItems: TechNews) {
        whenever(
            techNewsRepository.findByStatusAndRetryCountLessThan(
                eq(TechNewsStatus.COLLECTED),
                eq(TechNewsSummarizeService.MAX_RETRY),
                any(),
            )
        ).thenReturn(newsItems.toList())
    }

    @Test
    fun `크롤링한 본문을 요약해 SUMMARIZED로 전환한다`() {
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).thenReturn("본문 텍스트")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "한국어 요약"))

        val result = service.summarize()

        assertEquals(1, result.targetCount)
        assertEquals(1, result.summarizedCount)
        assertEquals(0, result.failedCount)
        assertEquals("한국어 요약", target.summary)
        assertEquals(TechNewsStatus.SUMMARIZED, target.status)
        verify(techNewsRepository).save(target)
        // 카테고리 마커 없는 출력 → '기타'(id 7) 폴백 매핑
        verify(techNewsCategoryMappingRepository).deleteByNewsIdBulk(target.id)
        verify(techNewsCategoryMappingRepository).saveAll(org.mockito.kotlin.check<List<TechNewsCategoryMapping>> {
            assertEquals(listOf(7L), it.map { m -> m.categoryId })
        })
    }

    @Test
    fun `LLM 출력의 카테고리 섹션을 분리해 매핑을 저장하고 summary에서 제거한다`() {
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).thenReturn(
            LlmCompletion("gemini", "**📌 한 줄 요약**\n코루틴 해설.\n\n**🏷️ 카테고리**\nBackend, DevOps")
        )

        service.summarize()

        assertEquals("**📌 한 줄 요약**\n코루틴 해설.", target.summary)
        verify(techNewsCategoryMappingRepository).deleteByNewsIdBulk(target.id)
        verify(techNewsCategoryMappingRepository).saveAll(org.mockito.kotlin.check<List<TechNewsCategoryMapping>> {
            assertEquals(listOf(2L, 4L), it.map { m -> m.categoryId })
            assertEquals(listOf(target.id, target.id), it.map { m -> m.newsId })
        })
    }

    @Test
    fun `프롬프트에 제목과 본문이 포함된다`() {
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).thenReturn("본문 텍스트")
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
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).doThrow(RuntimeException("403 Forbidden"))

        val result = service.summarize()

        assertEquals(1, result.failedCount)
        assertEquals(1, target.retryCount)
        assertEquals(TechNewsStatus.COLLECTED, target.status)
        assertNull(target.summary)
        verify(llmClient, never()).complete(any())
        verify(techNewsRepository).save(target)
        verify(techNewsCategoryMappingRepository, never()).saveAll(any<List<TechNewsCategoryMapping>>())
    }

    @Test
    fun `재시도 한도에 도달하면 FAILED로 전환한다`() {
        val target = news(retryCount = TechNewsSummarizeService.MAX_RETRY - 1)
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).doThrow(RuntimeException("403 Forbidden"))

        service.summarize()

        assertEquals(TechNewsSummarizeService.MAX_RETRY, target.retryCount)
        assertEquals(TechNewsStatus.FAILED, target.status)
    }

    @Test
    fun `FAILED 확정 시 Discord로 경고를 발송한다`() {
        val target = news(retryCount = TechNewsSummarizeService.MAX_RETRY - 1)
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).doThrow(RuntimeException("403 Forbidden"))

        service.summarize()

        verify(notifier).send(eq(DiscordNotifier.CHANNEL_TECH), org.mockito.kotlin.check {
            kotlin.test.assertTrue(it.contains("요약 최종 실패"))
            kotlin.test.assertTrue(it.contains(target.title))
            kotlin.test.assertTrue(it.contains("403 Forbidden"))
        })
    }

    @Test
    fun `재시도가 남아 있는 실패는 Discord 경고를 발송하지 않는다`() {
        val target = news(retryCount = 0)
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).doThrow(RuntimeException("timeout"))

        service.summarize()

        verify(notifierProvider, never()).ifAvailable(any())
    }

    @Test
    fun `LLM 실패도 재시도 대상으로 처리한다`() {
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).doThrow(IllegalStateException("모든 LLM 제공자 호출 실패"))

        val result = service.summarize()

        assertEquals(1, result.failedCount)
        assertEquals(1, target.retryCount)
        assertEquals(TechNewsStatus.COLLECTED, target.status)
    }

    @Test
    fun `한 건이 실패해도 나머지는 계속 처리한다`() {
        val failing = news(id = 1)
        val healthy = news(id = 2)
        stubTargets(failing, healthy)
        whenever(newsContentCrawler.fetch(failing.link)).doThrow(RuntimeException("timeout"))
        whenever(newsContentCrawler.fetch(healthy.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("groq", "요약"))

        val result = service.summarize()

        assertEquals(2, result.targetCount)
        assertEquals(1, result.summarizedCount)
        assertEquals(1, result.failedCount)
        assertEquals(TechNewsStatus.SUMMARIZED, healthy.status)
    }

    @Test
    fun `요약 성공한 글은 Discord embed로 발송한다`() {
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).thenReturn("본문")
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
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).doThrow(RuntimeException("403"))

        service.summarize()

        verify(notifierProvider, never()).ifAvailable(any())
    }

    @Test
    fun `요약 대상이 없으면 아무것도 하지 않는다`() {
        stubTargets()

        val result = service.summarize()

        assertEquals(0, result.targetCount)
        verify(techNewsRepository, never()).save(any())
    }

    @Test
    fun `SUMMARIZED 전환이 1건 이상이면 첫 페이지 캐시를 배치당 1회만 삭제한다`() {
        val first = news(id = 1)
        val second = news(id = 2)
        stubTargets(first, second)
        whenever(newsContentCrawler.fetch(any())).thenReturn("본문")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "요약"))

        service.summarize()

        // 전환 2건이어도 삭제는 배치 단위 1회 (건별 아님). 키 열거 스코프는 활성 카테고리 마스터 전체
        verify(cacheInvalidator, org.mockito.kotlin.times(1)).clear(org.mockito.kotlin.check {
            assertEquals(setOf(2L, 4L, 7L), it.toSet())
        })
    }

    @Test
    fun `일부만 성공한 배치도 캐시를 1회 삭제한다`() {
        val failing = news(id = 1)
        val healthy = news(id = 2)
        stubTargets(failing, healthy)
        whenever(newsContentCrawler.fetch(failing.link)).doThrow(RuntimeException("timeout"))
        whenever(newsContentCrawler.fetch(healthy.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("groq", "요약"))

        service.summarize()

        verify(cacheInvalidator, org.mockito.kotlin.times(1)).clear(any())
    }

    @Test
    fun `요약 대상이 없으면 캐시를 삭제하지 않는다`() {
        stubTargets()

        service.summarize()

        verify(cacheInvalidator, never()).clear(any())
    }

    @Test
    fun `전환 0건(전부 재시도 실패) 배치는 캐시를 삭제하지 않는다`() {
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).doThrow(RuntimeException("timeout"))

        service.summarize()

        verify(cacheInvalidator, never()).clear(any())
    }

    @Test
    fun `전부 FAILED로 확정된 배치는 캐시를 삭제하지 않는다`() {
        val target = news(retryCount = TechNewsSummarizeService.MAX_RETRY - 1)
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).doThrow(RuntimeException("403 Forbidden"))

        service.summarize()

        assertEquals(TechNewsStatus.FAILED, target.status)
        verify(cacheInvalidator, never()).clear(any())
    }

    @Test
    fun `캐시 삭제가 실패해도 배치 결과는 정상이다`() {
        // 실제 Invalidator + Redis 예외를 던지는 템플릿 — runCatching이 삼키는 실동작 검증
        val stringRedisTemplate: org.springframework.data.redis.core.StringRedisTemplate = mock {
            on { unlink(any<Collection<String>>()) } doThrow
                org.springframework.data.redis.RedisConnectionFailureException("connection refused")
        }
        val serviceWithRealInvalidator = TechNewsSummarizeService(
            techNewsRepository,
            techNewsCategoryMappingRepository,
            cmsApiClient,
            newsContentCrawler,
            llmClient,
            notifierProvider,
            transactionOperations,
            TechNewsFirstPageCacheInvalidator(stringRedisTemplate),
            newsIndexServiceProvider,
        )
        val target = news()
        stubTargets(target)
        whenever(newsContentCrawler.fetch(target.link)).thenReturn("본문")
        whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "요약"))

        val result = serviceWithRealInvalidator.summarize()

        assertEquals(1, result.summarizedCount)
        assertEquals(0, result.failedCount)
        assertEquals(TechNewsStatus.SUMMARIZED, target.status)
        verify(stringRedisTemplate).unlink(any<Collection<String>>())
    }

    @Test
    fun `LLM 미구성이면 조회 없이 스킵한다`() {
        whenever(llmClient.available).thenReturn(false)

        val result = service.summarize()

        assertEquals(0, result.targetCount)
        verify(techNewsRepository, never()).findByStatusAndRetryCountLessThan(any(), any(), any())
    }

    @Nested
    inner class 검색_색인 {

        @Test
        fun `요약된 건들을 배치 끝에 한 번만 색인한다`() {
            // 건별로 색인하면 bulk 왕복이 건수만큼 늘고, 커밋 시점이 뒤섞여
            // 아직 커밋되지 않은 건을 읽을 수 있다 (캐시 무효화와 같은 원칙)
            val targets = listOf(news(1), news(2))
            stubTargets(*targets.toTypedArray())
            targets.forEach { whenever(newsContentCrawler.fetch(it.link)).thenReturn("본문") }
            whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "요약"))

            service.summarize()

            verify(newsIndexService, org.mockito.kotlin.times(1)).index(listOf(1L, 2L))
        }

        @Test
        fun `요약 전환이 없으면 색인하지 않는다`() {
            val target = news()
            stubTargets(target)
            whenever(newsContentCrawler.fetch(target.link)).thenThrow(RuntimeException("크롤링 실패"))

            service.summarize()

            verify(newsIndexService, never()).index(any<List<Long>>())
        }

        @Test
        fun `색인이 실패해도 요약 배치는 성공으로 끝난다`() {
            // 요약은 status로 남고 어드민 인덱싱이 보험이다 — 색인 실패가 배치를 실패로 만들면 안 된다
            val target = news()
            stubTargets(target)
            whenever(newsContentCrawler.fetch(target.link)).thenReturn("본문")
            whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "요약"))
            whenever(newsIndexService.index(any<List<Long>>())).thenThrow(RuntimeException("opensearch down"))

            val result = service.summarize()

            assertEquals(1, result.summarizedCount)
            assertEquals(0, result.failedCount)
        }

        @Test
        fun `검색이 꺼진 환경이면 색인을 건너뛴다`() {
            // opensearch.main.enabled=false면 색인 서비스 빈이 없다 (조건부 빈)
            val emptyProvider: ObjectProvider<TechNewsIndexService> = mock {
                on { ifAvailable } doReturn null
            }
            val serviceWithoutIndex = TechNewsSummarizeService(
                techNewsRepository,
                techNewsCategoryMappingRepository,
                cmsApiClient,
                newsContentCrawler,
                llmClient,
                notifierProvider,
                transactionOperations,
                cacheInvalidator,
                emptyProvider,
            )
            val target = news()
            stubTargets(target)
            whenever(newsContentCrawler.fetch(target.link)).thenReturn("본문")
            whenever(llmClient.complete(any())).thenReturn(LlmCompletion("gemini", "요약"))

            val result = serviceWithoutIndex.summarize()

            assertEquals(1, result.summarizedCount)
            verify(newsIndexService, never()).index(any<List<Long>>())
        }
    }
}
