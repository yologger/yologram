package link.yologram.worker.domain.tech.article.service

import link.yologram.worker.domain.tech.article.client.ArticleContentCrawler
import link.yologram.worker.domain.tech.article.entity.TechArticle
import link.yologram.worker.domain.tech.article.enums.TechArticleStatus
import link.yologram.worker.domain.tech.article.repository.TechArticleCategoryMappingRepository
import link.yologram.worker.domain.tech.article.repository.TechArticleRepository
import link.yologram.worker.global.llm.LlmClient
import link.yologram.worker.global.llm.LlmCompletion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * 요약 파이프라인 통합 테스트 — 의도적으로 @Transactional 없이 실행해
 * 프로덕션과 동일한 트랜잭션 경계를 검증한다.
 * (@Modifying 벌크 delete가 트랜잭션 없이 호출되던 버그는 단위 테스트로는 잡히지 않았음)
 */
@SpringBootTest
@ActiveProfiles("test")
class TechArticleSummarizeIntegrationTest {

    @Autowired
    lateinit var service: TechArticleSummarizeService

    @Autowired
    lateinit var techArticleRepository: TechArticleRepository

    @Autowired
    lateinit var mappingRepository: TechArticleCategoryMappingRepository

    @MockitoBean
    lateinit var articleContentCrawler: ArticleContentCrawler

    @MockitoBean
    lateinit var llmClient: LlmClient

    @AfterEach
    fun cleanUp() {
        mappingRepository.deleteAll()
        techArticleRepository.deleteAll()
    }

    @Test
    fun `요약 성공 시 실제 트랜잭션 경계에서 SUMMARIZED 전환과 매핑 교체가 커밋된다`() {
        val article = techArticleRepository.save(
            TechArticle(
                sourceId = 1,
                title = "코루틴 딥다이브",
                link = "https://tech.example.com/posts/1",
                sourceName = "테크 블로그",
                publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0),
            )
        )
        whenever(llmClient.available).thenReturn(true)
        whenever(articleContentCrawler.fetch(article.link)).thenReturn("본문 텍스트")
        whenever(llmClient.complete(any())).thenReturn(
            LlmCompletion("gemini", "**📌 한 줄 요약**\n코루틴 해설.\n\n**🏷️ 카테고리**\nBackend, DevOps")
        )

        val result = service.summarize()

        assertEquals(1, result.summarizedCount)
        val saved = techArticleRepository.findById(article.id).orElseThrow()
        assertEquals(TechArticleStatus.SUMMARIZED, saved.status)
        assertEquals("**📌 한 줄 요약**\n코루틴 해설.", saved.summary)
        assertEquals(listOf("Backend", "DevOps"), mappingRepository.findByArticleId(article.id).map { it.category })
    }

    @Test
    fun `재요약 시 기존 매핑이 교체된다 (트랜잭션 내 벌크 delete + insert)`() {
        val article = techArticleRepository.save(
            TechArticle(
                sourceId = 1,
                title = "재요약 대상",
                link = "https://tech.example.com/posts/2",
                sourceName = "테크 블로그",
                publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0),
            )
        )
        whenever(llmClient.available).thenReturn(true)
        whenever(articleContentCrawler.fetch(article.link)).thenReturn("본문")
        whenever(llmClient.complete(any()))
            .thenReturn(LlmCompletion("gemini", "요약1\n\n**🏷️ 카테고리**\nCloud"))

        service.summarize()
        assertEquals(listOf("Cloud"), mappingRepository.findByArticleId(article.id).map { it.category })

        // 재요약 (상태 리셋 후 다른 분류)
        techArticleRepository.save(
            techArticleRepository.findById(article.id).orElseThrow().let {
                TechArticle(
                    id = it.id, sourceId = it.sourceId, title = it.title, link = it.link,
                    sourceName = it.sourceName, publishedAt = it.publishedAt,
                    status = TechArticleStatus.COLLECTED, retryCount = 0,
                    createdAt = it.createdAt,
                )
            }
        )
        whenever(llmClient.complete(any()))
            .thenReturn(LlmCompletion("gemini", "요약2\n\n**🏷️ 카테고리**\nSecurity, Frontend"))

        service.summarize()

        assertEquals(
            listOf("Frontend", "Security"),
            mappingRepository.findByArticleId(article.id).map { it.category }.sorted(),
        )
    }
}
