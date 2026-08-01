package link.yologram.worker.domain.news.tech.repository

import link.yologram.worker.domain.news.tech.entity.TechNews
import link.yologram.worker.domain.news.tech.entity.TechNewsSource
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
class TechNewsRepositoryTest {

    @Autowired
    lateinit var techNewsRepository: TechNewsRepository

    @Autowired
    lateinit var techNewsSourceRepository: TechNewsSourceRepository

    private fun news(link: String) = TechNews(
        sourceId = 1,
        title = "제목",
        link = link,
        sourceName = "테크 블로그",
        publishedAt = LocalDateTime.of(2026, 7, 6, 9, 0),
    )

    @Test
    fun `findExistingLinks는 저장된 link만 반환한다`() {
        techNewsRepository.saveAll(listOf(news("https://a/1"), news("https://a/2")))

        val existing = techNewsRepository.findExistingLinks(listOf("https://a/1", "https://a/2", "https://a/3"))

        assertEquals(setOf("https://a/1", "https://a/2"), existing.toSet())
    }

    @Test
    fun `findExistingLinks는 일치하는 link가 없으면 빈 목록을 반환한다`() {
        val existing = techNewsRepository.findExistingLinks(listOf("https://none/1"))

        assertTrue(existing.isEmpty())
    }

    @Test
    fun `findByStatusAndRetryCountLessThan은 상태·재시도 조건과 배치 크기를 지킨다`() {
        val a1 = techNewsRepositorySaveWith(link = "https://s/1", retryCount = 0)
        techNewsRepositorySaveWith(link = "https://s/2", retryCount = 3) // 재시도 한도 도달 — 제외
        val a3 = techNewsRepositorySaveWith(link = "https://s/3", retryCount = 2)
        val summarized = techNewsRepositorySaveWith(link = "https://s/4", retryCount = 0)
        summarized.status = link.yologram.worker.domain.news.tech.enums.TechNewsStatus.SUMMARIZED
        techNewsRepository.save(summarized) // 이미 요약됨 — 제외

        val targets = techNewsRepository.findByStatusAndRetryCountLessThan(
            link.yologram.worker.domain.news.tech.enums.TechNewsStatus.COLLECTED,
            3,
            org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("id")),
        )

        assertEquals(listOf(a1.id, a3.id), targets.map { it.id })
    }

    private fun techNewsRepositorySaveWith(link: String, retryCount: Int) =
        techNewsRepository.save(
            TechNews(
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
        techNewsSourceRepository.saveAll(
            listOf(
                TechNewsSource(name = "활성", url = "https://a/feed"),
                TechNewsSource(name = "비활성", url = "https://b/feed", isActive = false),
            )
        )

        val active = techNewsSourceRepository.findByIsActiveTrue()

        assertEquals(listOf("활성"), active.map { it.name })
    }
}
