package link.yologram.api.v1.domain.search.tech.resource

import link.yologram.api.v1.config.security.AdminJwtProperties
import link.yologram.api.v1.config.security.JwtProperties
import link.yologram.api.v1.domain.pms.tech.model.TechPostDetailResponse
import link.yologram.api.v1.domain.pms.tech.model.TechPostMetrics
import link.yologram.api.v1.domain.pms.tech.model.TechPostSummaryResponse
import link.yologram.api.v1.domain.search.exception.BlankSearchKeywordException
import link.yologram.api.v1.domain.search.exception.SearchExceptionHandler
import link.yologram.api.v1.domain.search.exception.SearchPageTooDeepException
import link.yologram.api.v1.domain.search.tech.model.TechPostSearchSort
import link.yologram.api.v1.domain.search.tech.service.TechPostSearchService
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
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
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime

@WebMvcTest(TechPostSearchResource::class)
// 검색 API·서비스는 조건부 빈(opensearch.main.enabled) — 켜지 않으면 컨트롤러가 등록되지 않아 404가 된다.
// 실제 OpenSearch에는 붙지 않는다(서비스가 MockitoBean)
@TestPropertySource(properties = ["opensearch.main.enabled=true"])
@Import(
    SearchExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
    AuthenticatedAdminUserResolver::class,
    OptionalAuthenticatedUserResolver::class,
)
class TechPostSearchResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var techPostSearchService: TechPostSearchService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    private val baseUrl = "/api/v1/search/tech/posts"

    private fun page(vararg items: TechPostSummaryResponse) = ApiEnvelopPage(
        data = items.toList(),
        page = 0,
        size = 10,
        totalPages = 1,
        totalCount = items.size.toLong(),
        first = true,
        last = true,
    )

    private fun summary(id: Long = 1200) = TechPostSummaryResponse(
        id = id,
        author = TechPostDetailResponse.Author(uid = 12, nickname = "tester0"),
        title = "제목",
        content = "본문",
        categoryIds = listOf(1),
        metrics = TechPostMetrics(commentCount = 2, likeCount = 3, viewCount = 4, likedByMe = false),
        createdAt = LocalDateTime.of(2026, 7, 18, 14, 23, 50),
    )

    @Nested
    inner class 검색 {

        @Test
        fun `200과 페이지 응답을 반환한다`() {
            whenever(techPostSearchService.search(any(), any(), any(), any(), isNull()))
                .thenReturn(page(summary()))

            mockMvc.get(baseUrl) { param("q", "제미나이") }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].id") { value(1200) }
                jsonPath("$.data[0].author.nickname") { value("tester0") }
                jsonPath("$.data[0].metrics.viewCount") { value(4) }
                jsonPath("$.totalCount") { value(1) }
                jsonPath("$.totalPages") { value(1) }
                jsonPath("$.first") { value(true) }
            }
        }

        @Test
        fun `page·size·sort 기본값은 0·10·RELEVANCE다`() {
            whenever(techPostSearchService.search(any(), any(), any(), any(), isNull())).thenReturn(page())

            mockMvc.get(baseUrl) { param("q", "제미나이") }.andExpect { status { isOk() } }

            verify(techPostSearchService).search(
                keyword = eq("제미나이"),
                page = eq(0),
                size = eq(10),
                sort = eq(TechPostSearchSort.RELEVANCE),
                viewerUid = isNull(),
            )
        }

        @Test
        fun `전달한 페이징·정렬을 그대로 넘긴다`() {
            whenever(techPostSearchService.search(any(), any(), any(), any(), isNull())).thenReturn(page())

            mockMvc.get(baseUrl) {
                param("q", "제미나이")
                param("page", "2")
                param("size", "20")
                param("sort", "LATEST")
            }.andExpect { status { isOk() } }

            verify(techPostSearchService).search(
                keyword = eq("제미나이"),
                page = eq(2),
                size = eq(20),
                sort = eq(TechPostSearchSort.LATEST),
                viewerUid = isNull(),
            )
        }

        @Test
        fun `q가 없으면 400이다`() {
            mockMvc.get(baseUrl).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `없는 정렬 값이면 400이다`() {
            mockMvc.get(baseUrl) {
                param("q", "제미나이")
                param("sort", "POPULAR")
            }.andExpect { status { isBadRequest() } }
        }
    }

    @Nested
    inner class 선택_인증 {

        @Test
        fun `토큰이 있으면 viewerUid를 넘긴다 (likedByMe 계산용)`() {
            whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(12L)
            whenever(techPostSearchService.search(any(), any(), any(), any(), eq(12L))).thenReturn(page())

            mockMvc.get(baseUrl) {
                param("q", "제미나이")
                header("Authorization", "Bearer valid-token")
            }.andExpect { status { isOk() } }

            verify(techPostSearchService).search(any(), any(), any(), any(), eq(12L))
        }

        @Test
        fun `토큰이 없으면 비로그인으로 처리한다`() {
            whenever(techPostSearchService.search(any(), any(), any(), any(), isNull())).thenReturn(page())

            mockMvc.get(baseUrl) { param("q", "제미나이") }.andExpect { status { isOk() } }

            verify(techPostSearchService).search(any(), any(), any(), any(), isNull())
        }

        @Test
        fun `토큰이 유효하지 않으면 401이다`() {
            // 선택 인증이지만 헤더가 있으면 검증한다 — 무효 토큰을 비로그인으로 흘리지 않는다
            whenever(jwtUtil.validateAndGetUid("bad-token")).thenThrow(AuthTokenInvalidException())

            mockMvc.get(baseUrl) {
                param("q", "제미나이")
                header("Authorization", "Bearer bad-token")
            }.andExpect { status { isUnauthorized() } }
        }
    }

    @Nested
    inner class 예외 {

        @Test
        fun `검색어가 비면 400 BLANK_SEARCH_KEYWORD`() {
            whenever(techPostSearchService.search(any(), any(), any(), any(), isNull()))
                .thenThrow(BlankSearchKeywordException())

            mockMvc.get(baseUrl) { param("q", "  ") }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("BLANK_SEARCH_KEYWORD") }
            }
        }

        @Test
        fun `조회 한계를 넘으면 400 SEARCH_PAGE_TOO_DEEP`() {
            whenever(techPostSearchService.search(any(), any(), any(), any(), isNull()))
                .thenThrow(SearchPageTooDeepException())

            mockMvc.get(baseUrl) {
                param("q", "제미나이")
                param("page", "5000")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("SEARCH_PAGE_TOO_DEEP") }
            }
        }
    }
}
