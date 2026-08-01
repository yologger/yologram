package link.yologram.api.v1.domain.news.tech.repository

import link.yologram.api.v1.domain.news.tech.entity.TechNewsSource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TechNewsSourceRepositoryTest {

    @Autowired
    lateinit var techNewsSourceRepository: TechNewsSourceRepository

    private fun createSource(
        name: String = "GeekNews",
        url: String = "https://news.hada.io/rss/news",
        isActive: Boolean = true,
    ) = TechNewsSource(name = name, url = url, isActive = isActive)

    @Nested
    inner class 소스_저장 {

        @Test
        fun `저장 성공 시 id가 생성된다`() {
            val saved = techNewsSourceRepository.save(createSource())

            assertTrue(saved.id > 0)
        }

        @Test
        fun `저장된 소스의 필드값이 정확하다`() {
            val saved = techNewsSourceRepository.save(createSource())

            assertEquals("GeekNews", saved.name)
            assertEquals("https://news.hada.io/rss/news", saved.url)
            assertTrue(saved.isActive)
        }

        @Test
        fun `저장 시 createdAt과 modifiedDate가 채워진다`() {
            val saved = techNewsSourceRepository.saveAndFlush(createSource())

            assertNotNull(saved.createdAt)
            assertNotNull(saved.modifiedDate)
        }

        @Test
        fun `isActive false로 저장할 수 있다`() {
            val saved = techNewsSourceRepository.save(createSource(isActive = false))

            assertFalse(saved.isActive)
        }
    }

    @Nested
    inner class 전체_목록_조회 {

        @Test
        fun `id 오름차순으로 반환한다`() {
            val first = techNewsSourceRepository.save(createSource(name = "소스1", url = "https://example.com/rss/1"))
            val second = techNewsSourceRepository.save(createSource(name = "소스2", url = "https://example.com/rss/2"))
            val third = techNewsSourceRepository.save(createSource(name = "소스3", url = "https://example.com/rss/3"))

            val found = techNewsSourceRepository.findAllByOrderByIdAsc()
                .filter { it.id in listOf(first.id, second.id, third.id) }

            assertEquals(listOf(first.id, second.id, third.id), found.map { it.id })
        }

        @Test
        fun `저장된 소스가 없으면 빈 리스트를 반환한다`() {
            val found = techNewsSourceRepository.findAllByOrderByIdAsc()

            assertTrue(found.isEmpty())
        }
    }

    @Nested
    inner class URL_중복_확인 {

        @Test
        fun `존재하는 url은 true`() {
            techNewsSourceRepository.save(createSource())

            assertTrue(techNewsSourceRepository.existsByUrl("https://news.hada.io/rss/news"))
        }

        @Test
        fun `존재하지 않는 url은 false`() {
            assertFalse(techNewsSourceRepository.existsByUrl("https://notfound.example.com/rss"))
        }
    }

    @Nested
    inner class 자기_자신_제외_URL_중복_확인 {

        @Test
        fun `같은 url이 자기 자신뿐이면 false`() {
            val saved = techNewsSourceRepository.save(createSource())

            assertFalse(techNewsSourceRepository.existsByUrlAndIdNot(saved.url, saved.id))
        }

        @Test
        fun `다른 소스가 같은 url을 가지면 true`() {
            techNewsSourceRepository.save(createSource(name = "기존", url = "https://example.com/rss/dup"))
            val other = techNewsSourceRepository.save(createSource(name = "수정대상", url = "https://example.com/rss/other"))

            assertTrue(techNewsSourceRepository.existsByUrlAndIdNot("https://example.com/rss/dup", other.id))
        }

        @Test
        fun `아무도 같은 url을 갖지 않으면 false`() {
            val saved = techNewsSourceRepository.save(createSource())

            assertFalse(techNewsSourceRepository.existsByUrlAndIdNot("https://notfound.example.com/rss", saved.id))
        }
    }

    @Nested
    inner class 소스_삭제 {

        @Test
        fun `삭제하면 조회되지 않는다`() {
            val saved = techNewsSourceRepository.saveAndFlush(createSource())

            techNewsSourceRepository.delete(saved)

            assertTrue(techNewsSourceRepository.findById(saved.id).isEmpty)
        }
    }
}
