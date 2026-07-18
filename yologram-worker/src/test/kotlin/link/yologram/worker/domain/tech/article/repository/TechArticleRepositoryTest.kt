package link.yologram.worker.domain.tech.article.repository

import link.yologram.worker.domain.tech.article.entity.TechArticle
import link.yologram.worker.domain.tech.article.entity.TechArticleSource
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
class TechArticleRepositoryTest {

    @Autowired
    lateinit var techArticleRepository: TechArticleRepository

    @Autowired
    lateinit var techArticleSourceRepository: TechArticleSourceRepository

    private fun article(link: String) = TechArticle(
        sourceId = 1,
        title = "제목",
        link = link,
        sourceName = "테크 블로그",
        publishedAt = LocalDateTime.of(2026, 7, 6, 9, 0),
    )

    @Test
    fun `findExistingLinks는 저장된 link만 반환한다`() {
        techArticleRepository.saveAll(listOf(article("https://a/1"), article("https://a/2")))

        val existing = techArticleRepository.findExistingLinks(listOf("https://a/1", "https://a/2", "https://a/3"))

        assertEquals(setOf("https://a/1", "https://a/2"), existing.toSet())
    }

    @Test
    fun `findExistingLinks는 일치하는 link가 없으면 빈 목록을 반환한다`() {
        val existing = techArticleRepository.findExistingLinks(listOf("https://none/1"))

        assertTrue(existing.isEmpty())
    }

    @Test
    fun `findByStatusAndRetryCountLessThan은 상태·재시도 조건과 배치 크기를 지킨다`() {
        val a1 = techNewsRepositorySaveWith(link = "https://s/1", retryCount = 0)
        techNewsRepositorySaveWith(link = "https://s/2", retryCount = 3) // 재시도 한도 도달 — 제외
        val a3 = techNewsRepositorySaveWith(link = "https://s/3", retryCount = 2)
        val summarized = techNewsRepositorySaveWith(link = "https://s/4", retryCount = 0)
        summarized.status = link.yologram.worker.domain.tech.article.enums.TechArticleStatus.SUMMARIZED
        techArticleRepository.save(summarized) // 이미 요약됨 — 제외

        val targets = techArticleRepository.findByStatusAndRetryCountLessThan(
            link.yologram.worker.domain.tech.article.enums.TechArticleStatus.COLLECTED,
            3,
            org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("id")),
        )

        assertEquals(listOf(a1.id, a3.id), targets.map { it.id })
    }

    private fun techNewsRepositorySaveWith(link: String, retryCount: Int) =
        techArticleRepository.save(
            TechArticle(
                sourceId = 1,
                title = "제목",
                link = link,
                sourceName = "테크 블로그",
                publishedAt = LocalDateTime.of(2026, 7, 6, 9, 0),
                retryCount = retryCount,
            )
        )

    @Test
    fun `findByIsActiveTrue는 활성 소스만 반환한다`() {
        techArticleSourceRepository.saveAll(
            listOf(
                TechArticleSource(name = "활성", url = "https://a/feed"),
                TechArticleSource(name = "비활성", url = "https://b/feed", isActive = false),
            )
        )

        val active = techArticleSourceRepository.findByIsActiveTrue()

        assertEquals(listOf("활성"), active.map { it.name })
    }
}
