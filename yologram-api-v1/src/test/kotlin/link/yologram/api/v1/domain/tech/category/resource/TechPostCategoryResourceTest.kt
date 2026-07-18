package link.yologram.api.v1.domain.tech.category.resource

import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.tech.category.model.TechPostCategoryResponse
import link.yologram.api.v1.domain.tech.category.service.TechPostCategoryService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(TechPostCategoryResource::class)
@Import(GlobalExceptionHandler::class, AuthenticatedUserResolver::class)
class TechPostCategoryResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var categoryService: TechPostCategoryService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @Test
    fun `200과 카테고리 목록을 반환한다`() {
        whenever(categoryService.getCategories()).thenReturn(
            listOf(
                TechPostCategoryResponse(id = 1L, name = "Frontend", sortOrder = 1),
                TechPostCategoryResponse(id = 2L, name = "Backend", sortOrder = 2),
            )
        )

        mockMvc.get("/api/v1/cms/tech/categories")
            .andExpect {
                status { isOk() }
                jsonPath("$.data[0].id") { value(1) }
                jsonPath("$.data[0].name") { value("Frontend") }
                jsonPath("$.data[0].sortOrder") { value(1) }
                jsonPath("$.data[1].name") { value("Backend") }
            }
    }

    @Test
    fun `카테고리가 없으면 빈 배열을 반환한다`() {
        whenever(categoryService.getCategories()).thenReturn(emptyList())

        mockMvc.get("/api/v1/cms/tech/categories")
            .andExpect {
                status { isOk() }
                jsonPath("$.data") { isEmpty() }
            }
    }

    @Test
    fun `tech가 아닌 섹션 경로는 매핑이 없어 404 반환`() {
        // 구 /cms/{section}/categories의 section 경로변수는 tech 고정 매핑으로 전환됨 — 그 외 섹션 경로는 미매핑(404)
        mockMvc.get("/api/v1/cms/unknown/categories")
            .andExpect {
                status { isNotFound() }
            }
    }
}
