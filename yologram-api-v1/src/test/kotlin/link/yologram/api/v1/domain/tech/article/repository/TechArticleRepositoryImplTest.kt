package link.yologram.api.v1.domain.tech.article.repository

import link.yologram.api.v1.domain.tech.article.entity.TechArticle
import link.yologram.api.v1.domain.tech.article.entity.TechArticleCategoryMapping
import link.yologram.api.v1.domain.tech.article.enums.TechArticleStatus
import link.yologram.api.v1.domain.tech.article.model.TechArticleCursor
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechArticleRepositoryImplTest {

    @Autowired
    lateinit var techArticleRepository: TechArticleRepository

    @Autowired
    lateinit var mappingRepository: TechArticleCategoryMappingRepository

    private var seq = 0L

    private fun article(
        publishedAt: LocalDateTime,
        status: TechArticleStatus = TechArticleStatus.SUMMARIZED,
    ): TechArticle {
        seq += 1
        return techArticleRepository.save(
            TechArticle(
                id = seq,
                sourceId = 1,
                title = "제목 $seq",
                link = "https://a/$seq",
                summary = if (status == TechArticleStatus.SUMMARIZED) "요약 $seq" else null,
                sourceName = "테크 블로그",
                publishedAt = publishedAt,
                status = status,
            )
        )
    }

    private val base = LocalDateTime.of(2026, 7, 18, 9, 0, 0)

    @Test
    fun `SUMMARIZED만 발행순으로 반환한다`() {
        val old = article(base.minusDays(2))
        val recent = article(base)
        article(base.minusDays(1), status = TechArticleStatus.COLLECTED)
        article(base.minusDays(1), status = TechArticleStatus.FAILED)

        val result = techArticleRepository.findSummarizedArticles(null, null, 10)

        assertEquals(listOf(recent.id, old.id), result.map { it.id })
    }

    @Test
    fun `발행 시각이 같으면 id 내림차순으로 정렬된다`() {
        val first = article(base)
        val second = article(base)

        val result = techArticleRepository.findSummarizedArticles(null, null, 10)

        assertEquals(listOf(second.id, first.id), result.map { it.id })
    }

    @Test
    fun `커서 이후 페이지가 중복·누락 없이 이어진다 (동일 발행 시각 경계 포함)`() {
        // 같은 발행 시각 4건 + 다른 시각 2건 — 경계가 동일 시각 한가운데 걸리게 페이지 크기 3
        val articles = listOf(
            article(base.plusHours(1)),          // 최신
            article(base), article(base), article(base), article(base),
            article(base.minusHours(1)),         // 가장 과거
        )

        val page1 = techArticleRepository.findSummarizedArticles(null, null, 3)
        val cursor = page1.last().let { TechArticleCursor(it.publishedAt, it.id) }
        val page2 = techArticleRepository.findSummarizedArticles(null, cursor, 3)

        val all = (page1 + page2).map { it.id }
        assertEquals(articles.map { it.id }.toSet(), all.toSet())   // 누락 없음
        assertEquals(all.size, all.toSet().size)                    // 중복 없음
    }

    @Test
    fun `limit만큼만 반환한다`() {
        repeat(5) { article(base.plusMinutes(it.toLong())) }

        assertEquals(2, techArticleRepository.findSummarizedArticles(null, null, 2).size)
    }

    @Test
    fun `데이터가 없으면 빈 목록을 반환한다`() {
        assertTrue(techArticleRepository.findSummarizedArticles(null, null, 10).isEmpty())
    }

    @Test
    fun `category 필터는 해당 매핑이 있는 글만 반환한다`() {
        val backend = article(base)
        val cloudOnly = article(base.minusHours(1))
        mappingRepository.saveAll(
            listOf(
                TechArticleCategoryMapping(id = 1, articleId = backend.id, category = "Backend"),
                TechArticleCategoryMapping(id = 2, articleId = backend.id, category = "Cloud"),
                TechArticleCategoryMapping(id = 3, articleId = cloudOnly.id, category = "Cloud"),
            )
        )

        val result = techArticleRepository.findSummarizedArticles("Backend", null, 10)

        assertEquals(listOf(backend.id), result.map { it.id })
    }

    @Test
    fun `category 필터에 매칭이 없으면 빈 목록을 반환한다`() {
        article(base)

        assertTrue(techArticleRepository.findSummarizedArticles("Security", null, 10).isEmpty())
    }
}
