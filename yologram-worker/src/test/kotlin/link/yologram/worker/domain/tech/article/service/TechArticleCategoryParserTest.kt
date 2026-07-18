package link.yologram.worker.domain.tech.article.service

import link.yologram.worker.domain.tech.article.entity.TechCategory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TechArticleCategoryParserTest {

    // tech_category 마스터의 활성 어휘 (id는 커뮤니티 시드와 동일)
    private val vocabulary = listOf(
        TechCategory(id = 1, name = "Frontend", sortOrder = 1),
        TechCategory(id = 2, name = "Backend", sortOrder = 2),
        TechCategory(id = 3, name = "AI/ML", sortOrder = 3),
        TechCategory(id = 4, name = "DevOps", sortOrder = 4),
        TechCategory(id = 5, name = "Cloud", sortOrder = 5),
        TechCategory(id = 6, name = "Security", sortOrder = 6),
        TechCategory(id = 7, name = "기타", sortOrder = 7),
    )

    private val body = """
        **📌 한 줄 요약**
        코루틴 내부 구조 해설.

        **🔑 핵심 포인트**
        - CPS 변환으로 상태 머신 생성.
    """.trimIndent()

    @Test
    fun `카테고리 섹션을 분리하고 summary에서 제거한다`() {
        val output = "$body\n\n**🏷️ 카테고리**\nBackend, DevOps"

        val parsed = TechArticleCategoryParser.parse(output, vocabulary)

        assertEquals(listOf(2L, 4L), parsed.categoryIds)
        assertEquals(body, parsed.summary)
        assertFalse(parsed.summary.contains("카테고리"))
    }

    @Test
    fun `슬래시가 포함된 AI ML 라벨도 파싱된다`() {
        val parsed = TechArticleCategoryParser.parse("$body\n\n**🏷️ 카테고리**\nAI/ML", vocabulary)

        assertEquals(listOf(3L), parsed.categoryIds)
    }

    @Test
    fun `마커가 없으면 기타로 폴백하고 summary는 그대로다`() {
        val parsed = TechArticleCategoryParser.parse(body, vocabulary)

        assertEquals(listOf(7L), parsed.categoryIds)
        assertEquals(body, parsed.summary)
    }

    @Test
    fun `어휘 외 값은 무시하고 유효한 것만 남긴다`() {
        val parsed = TechArticleCategoryParser.parse("$body\n\n**🏷️ 카테고리**\nBlockchain, Backend, 데이터베이스", vocabulary)

        assertEquals(listOf(2L), parsed.categoryIds)
    }

    @Test
    fun `유효한 값이 하나도 없으면 기타로 폴백한다`() {
        val parsed = TechArticleCategoryParser.parse("$body\n\n**🏷️ 카테고리**\nBlockchain, Quantum", vocabulary)

        assertEquals(listOf(7L), parsed.categoryIds)
    }

    @Test
    fun `어휘에 기타가 없으면 폴백은 빈 목록이다`() {
        val withoutEtc = vocabulary.filter { it.name != "기타" }

        val parsed = TechArticleCategoryParser.parse(body, withoutEtc)

        assertTrue(parsed.categoryIds.isEmpty())
    }

    @Test
    fun `4개 이상이면 앞의 3개만 취한다`() {
        val parsed = TechArticleCategoryParser.parse("$body\n\n**🏷️ 카테고리**\nFrontend, Backend, Cloud, Security", vocabulary)

        assertEquals(listOf(1L, 2L, 5L), parsed.categoryIds)
    }

    @Test
    fun `중복 라벨은 한 번만 남는다`() {
        val parsed = TechArticleCategoryParser.parse("$body\n\n**🏷️ 카테고리**\nBackend, backend, BACKEND", vocabulary)

        assertEquals(listOf(2L), parsed.categoryIds)
    }

    @Test
    fun `볼드 없는 마커·공백 변형도 파싱된다`() {
        val parsed = TechArticleCategoryParser.parse("$body\n\n🏷️ 카테고리: Cloud , Security", vocabulary)

        assertEquals(listOf(5L, 6L), parsed.categoryIds)
    }
}
