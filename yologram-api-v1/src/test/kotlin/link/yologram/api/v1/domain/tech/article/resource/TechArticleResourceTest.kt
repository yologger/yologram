package link.yologram.api.v1.domain.tech.article.resource

import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.tech.article.exception.InvalidTechArticleCursorException
import link.yologram.api.v1.domain.tech.article.exception.TechArticleExceptionHandler
import link.yologram.api.v1.domain.tech.article.model.TechArticleResponse
import link.yologram.api.v1.domain.tech.article.service.TechArticleService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime

@WebMvcTest(TechArticleResource::class)
@Import(GlobalExceptionHandler::class, TechArticleExceptionHandler::class, AuthenticatedUserResolver::class)
class TechArticleResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var techArticleService: TechArticleService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    private fun response(id: Long) = TechArticleResponse(
        id = id,
        title = "코틀린 코루틴 딥다이브",
        summary = "**📌 한 줄 요약** 코루틴 내부 구조 해설.",
        link = "https://tech.example.com/posts/$id",
        sourceName = "테크 블로그",
        categories = listOf("Backend", "DevOps"),
        publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0),
    )

    @Test
    fun `200과 아티클 목록을 반환한다`() {
        whenever(techArticleService.getArticlesByCursor(anyOrNull(), anyOrNull(), any())).thenReturn(
            ApiEnvelopCursorPage(data = listOf(response(1)), nextCursor = "next")
        )

        mockMvc.get("/api/v1/articles/tech")
            .andExpect {
                status { isOk() }
                jsonPath("$.data[0].id") { value(1) }
                jsonPath("$.data[0].title") { value("코틀린 코루틴 딥다이브") }
                jsonPath("$.data[0].summary") { value("**📌 한 줄 요약** 코루틴 내부 구조 해설.") }
                jsonPath("$.data[0].sourceName") { value("테크 블로그") }
                jsonPath("$.data[0].categories[0]") { value("Backend") }
                jsonPath("$.nextCursor") { value("next") }
            }
    }

    @Test
    fun `cursor·size 파라미터가 서비스로 전달된다`() {
        whenever(techArticleService.getArticlesByCursor(anyOrNull(), eq("abc"), eq(10))).thenReturn(
            ApiEnvelopCursorPage(data = emptyList(), nextCursor = null)
        )

        mockMvc.get("/api/v1/articles/tech?cursor=abc&size=10")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `categoryId 파라미터가 서비스로 전달된다`() {
        whenever(techArticleService.getArticlesByCursor(eq(2L), anyOrNull(), any())).thenReturn(
            ApiEnvelopCursorPage(data = emptyList(), nextCursor = null)
        )

        mockMvc.get("/api/v1/articles/tech?categoryId=2")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `잘못된 커서면 400 INVALID_CURSOR를 반환한다`() {
        whenever(techArticleService.getArticlesByCursor(anyOrNull(), anyOrNull(), any()))
            .thenThrow(InvalidTechArticleCursorException())

        mockMvc.get("/api/v1/articles/tech?cursor=broken")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_CURSOR") }
            }
    }
}
