package link.yologram.worker.domain.tech.article.service

import link.yologram.worker.domain.tech.article.enums.TechArticleCategory
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TechArticleCategoryParserTest {

    private val body = """
        **📌 한 줄 요약**
        코루틴 내부 구조 해설.

        **🔑 핵심 포인트**
        - CPS 변환으로 상태 머신 생성.
    """.trimIndent()

    @Test
    fun `카테고리 섹션을 분리하고 summary에서 제거한다`() {
        val output = "$body\n\n**🏷️ 카테고리**\nBackend, DevOps"

        val parsed = TechArticleCategoryParser.parse(output)

        assertEquals(listOf(TechArticleCategory.BACKEND, TechArticleCategory.DEVOPS), parsed.categories)
        assertEquals(body, parsed.summary)
        assertFalse(parsed.summary.contains("카테고리"))
    }

    @Test
    fun `슬래시가 포함된 AI ML 라벨도 파싱된다`() {
        val output = "$body\n\n**🏷️ 카테고리**\nAI/ML"

        val parsed = TechArticleCategoryParser.parse(output)

        assertEquals(listOf(TechArticleCategory.AI_ML), parsed.categories)
    }

    @Test
    fun `마커가 없으면 기타 하나로 폴백하고 summary는 그대로다`() {
        val parsed = TechArticleCategoryParser.parse(body)

        assertEquals(listOf(TechArticleCategory.ETC), parsed.categories)
        assertEquals(body, parsed.summary)
    }

    @Test
    fun `목록 외 값은 무시하고 유효한 것만 남긴다`() {
        val output = "$body\n\n**🏷️ 카테고리**\nBlockchain, Backend, 데이터베이스"

        val parsed = TechArticleCategoryParser.parse(output)

        assertEquals(listOf(TechArticleCategory.BACKEND), parsed.categories)
    }

    @Test
    fun `유효한 값이 하나도 없으면 기타로 폴백한다`() {
        val output = "$body\n\n**🏷️ 카테고리**\nBlockchain, Quantum"

        val parsed = TechArticleCategoryParser.parse(output)

        assertEquals(listOf(TechArticleCategory.ETC), parsed.categories)
    }

    @Test
    fun `4개 이상이면 앞의 3개만 취한다`() {
        val output = "$body\n\n**🏷️ 카테고리**\nFrontend, Backend, Cloud, Security"

        val parsed = TechArticleCategoryParser.parse(output)

        assertEquals(3, parsed.categories.size)
        assertEquals(
            listOf(TechArticleCategory.FRONTEND, TechArticleCategory.BACKEND, TechArticleCategory.CLOUD),
            parsed.categories,
        )
    }

    @Test
    fun `중복 라벨은 한 번만 남는다`() {
        val output = "$body\n\n**🏷️ 카테고리**\nBackend, backend, BACKEND"

        val parsed = TechArticleCategoryParser.parse(output)

        assertEquals(listOf(TechArticleCategory.BACKEND), parsed.categories)
    }

    @Test
    fun `볼드 없는 마커·공백 변형도 파싱된다`() {
        val output = "$body\n\n🏷️ 카테고리: Cloud , Security"

        val parsed = TechArticleCategoryParser.parse(output)

        assertEquals(listOf(TechArticleCategory.CLOUD, TechArticleCategory.SECURITY), parsed.categories)
    }
}
