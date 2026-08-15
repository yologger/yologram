package link.yologram.api.v1.domain.search.tech.resource

import link.yologram.api.v1.config.security.AdminJwtProperties
import link.yologram.api.v1.config.security.JwtProperties
import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.domain.search.exception.BlankSearchKeywordException
import link.yologram.api.v1.domain.search.exception.SearchExceptionHandler
import link.yologram.api.v1.domain.search.exception.SearchPageTooDeepException
import link.yologram.api.v1.domain.search.exception.SearchUnavailableException
import link.yologram.api.v1.domain.search.tech.model.TechSearchSort
import link.yologram.api.v1.domain.search.tech.service.TechNewsSearchService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.resolver.OptionalAuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import link.yologram.api.v1.global.exception.ValidationExceptionHandler
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime

@WebMvcTest(TechNewsSearchResource::class)
@Import(
    SearchExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
    AuthenticatedAdminUserResolver::class,
    OptionalAuthenticatedUserResolver::class,
)
class TechNewsSearchResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var techNewsSearchService: TechNewsSearchService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    private val baseUrl = "/api/v1/search/tech/news"

    private fun page(vararg items: TechNewsResponse) = ApiEnvelopPage(
        data = items.toList(),
        page = 0,
        size = 10,
        totalPages = 1,
        totalCount = items.size.toLong(),
        first = true,
        last = true,
    )

    private fun news(id: Long = 900) = TechNewsResponse(
        id = id,
        title = "제목",
        summary = "요약",
        link = "https://news.test/$id",
        sourceName = "GeekNews",
        categories = listOf("인프라"),
        publishedAt = LocalDateTime.of(2026, 7, 18, 14, 23, 50),
    )

    @Nested
    inner class 검색 {

        @Test
        fun `200과 페이지 응답을 반환한다`() {
            whenever(techNewsSearchService.search(any(), any(), any(), any())).thenReturn(page(news()))

            mockMvc.get(baseUrl) { param("q", "마이그레이션") }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].id") { value(900) }
                jsonPath("$.data[0].sourceName") { value("GeekNews") }
                jsonPath("$.data[0].categories[0]") { value("인프라") }
                jsonPath("$.totalCount") { value(1) }
                jsonPath("$.totalPages") { value(1) }
                jsonPath("$.first") { value(true) }
            }
        }

        @Test
        fun `page·size·sort 기본값은 0·10·RELEVANCE다`() {
            whenever(techNewsSearchService.search(any(), any(), any(), any())).thenReturn(page())

            mockMvc.get(baseUrl) { param("q", "마이그레이션") }.andExpect { status { isOk() } }

            verify(techNewsSearchService).search(
                keyword = eq("마이그레이션"),
                page = eq(0),
                size = eq(10),
                sort = eq(TechSearchSort.RELEVANCE),
            )
        }

        @Test
        fun `전달한 페이징·정렬을 그대로 넘긴다`() {
            whenever(techNewsSearchService.search(any(), any(), any(), any())).thenReturn(page())

            mockMvc.get(baseUrl) {
                param("q", "마이그레이션")
                param("page", "2")
                param("size", "20")
                param("sort", "LATEST")
            }.andExpect { status { isOk() } }

            verify(techNewsSearchService).search(
                keyword = eq("마이그레이션"),
                page = eq(2),
                size = eq(20),
                sort = eq(TechSearchSort.LATEST),
            )
        }

        @Test
        fun `q가 없으면 400이다`() {
            mockMvc.get(baseUrl).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `없는 정렬 값이면 400이다`() {
            mockMvc.get(baseUrl) {
                param("q", "마이그레이션")
                param("sort", "POPULAR")
            }.andExpect { status { isBadRequest() } }
        }

        @Test
        fun `인증 없이도 200이다`() {
            // 게시글 검색과 달리 개인화 값이 없어 토큰을 보지 않는다
            whenever(techNewsSearchService.search(any(), any(), any(), any())).thenReturn(page(news()))

            mockMvc.get(baseUrl) { param("q", "마이그레이션") }.andExpect { status { isOk() } }
        }
    }

    @Nested
    inner class 예외 {

        @Test
        fun `검색어가 비면 400 BLANK_SEARCH_KEYWORD`() {
            whenever(techNewsSearchService.search(any(), any(), any(), any()))
                .thenThrow(BlankSearchKeywordException())

            mockMvc.get(baseUrl) { param("q", "  ") }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("BLANK_SEARCH_KEYWORD") }
            }
        }

        @Test
        fun `검색 설정이 없으면 503 SEARCH_UNAVAILABLE`() {
            whenever(techNewsSearchService.search(any(), any(), any(), any()))
                .thenThrow(SearchUnavailableException())

            mockMvc.get(baseUrl) { param("q", "마이그레이션") }.andExpect {
                status { isServiceUnavailable() }
                jsonPath("$.errorCode") { value("SEARCH_UNAVAILABLE") }
            }
        }

        @Test
        fun `조회 한계를 넘으면 400 SEARCH_PAGE_TOO_DEEP`() {
            whenever(techNewsSearchService.search(any(), any(), any(), any()))
                .thenThrow(SearchPageTooDeepException())

            mockMvc.get(baseUrl) {
                param("q", "마이그레이션")
                param("page", "5000")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("SEARCH_PAGE_TOO_DEEP") }
            }
        }
    }
}
