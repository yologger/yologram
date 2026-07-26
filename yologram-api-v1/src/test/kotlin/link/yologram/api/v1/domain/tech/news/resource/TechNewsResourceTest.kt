package link.yologram.api.v1.domain.tech.news.resource

import link.yologram.api.v1.config.AdminJwtProperties
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.tech.news.exception.InvalidTechNewsCursorException
import link.yologram.api.v1.domain.tech.news.exception.TechNewsExceptionHandler
import link.yologram.api.v1.domain.tech.news.model.TechNewsResponse
import link.yologram.api.v1.domain.tech.news.service.TechNewsService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
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

@WebMvcTest(TechNewsResource::class)
@Import(GlobalExceptionHandler::class, TechNewsExceptionHandler::class, AuthenticatedUserResolver::class, AuthenticatedAdminUserResolver::class)
class TechNewsResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var techNewsService: TechNewsService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    private fun response(id: Long) = TechNewsResponse(
        id = id,
        title = "코틀린 코루틴 딥다이브",
        summary = "**📌 한 줄 요약** 코루틴 내부 구조 해설.",
        link = "https://tech.example.com/posts/$id",
        sourceName = "테크 블로그",
        categories = listOf("Backend", "DevOps"),
        publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0),
    )

    @Test
    fun `200과 뉴스 목록을 반환한다`() {
        whenever(techNewsService.getNewsByCursor(anyOrNull(), anyOrNull(), any())).thenReturn(
            ApiEnvelopCursorPage(data = listOf(response(1)), nextCursor = "next")
        )

        mockMvc.get("/api/v1/news/tech")
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
        whenever(techNewsService.getNewsByCursor(anyOrNull(), eq("abc"), eq(10))).thenReturn(
            ApiEnvelopCursorPage(data = emptyList(), nextCursor = null)
        )

        mockMvc.get("/api/v1/news/tech?cursor=abc&size=10")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `categoryId 파라미터가 서비스로 전달된다`() {
        whenever(techNewsService.getNewsByCursor(eq(2L), anyOrNull(), any())).thenReturn(
            ApiEnvelopCursorPage(data = emptyList(), nextCursor = null)
        )

        mockMvc.get("/api/v1/news/tech?categoryId=2")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `잘못된 커서면 400 INVALID_CURSOR를 반환한다`() {
        whenever(techNewsService.getNewsByCursor(anyOrNull(), anyOrNull(), any()))
            .thenThrow(InvalidTechNewsCursorException())

        mockMvc.get("/api/v1/news/tech?cursor=broken")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_CURSOR") }
            }
    }
}
