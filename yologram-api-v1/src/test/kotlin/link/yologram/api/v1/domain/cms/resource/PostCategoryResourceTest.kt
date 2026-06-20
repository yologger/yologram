package link.yologram.api.v1.domain.cms.resource

import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.cms.exception.CmsExceptionHandler
import link.yologram.api.v1.domain.cms.exception.InvalidSectionException
import link.yologram.api.v1.domain.cms.model.PostCategoryResponse
import link.yologram.api.v1.domain.cms.service.PostCategoryService
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

@WebMvcTest(PostCategoryResource::class)
@Import(CmsExceptionHandler::class, GlobalExceptionHandler::class, AuthenticatedUserResolver::class)
class PostCategoryResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var categoryService: PostCategoryService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @Test
    fun `200과 카테고리 목록을 반환한다`() {
        whenever(categoryService.getPostCategories("tech")).thenReturn(
            listOf(
                PostCategoryResponse(id = 1L, name = "Frontend", sortOrder = 1),
                PostCategoryResponse(id = 2L, name = "Backend", sortOrder = 2),
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
        whenever(categoryService.getPostCategories("politics")).thenReturn(emptyList())

        mockMvc.get("/api/v1/cms/politics/categories")
            .andExpect {
                status { isOk() }
                jsonPath("$.data") { isEmpty() }
            }
    }

    @Test
    fun `유효하지 않은 section이면 400을 반환한다`() {
        whenever(categoryService.getPostCategories("unknown")).thenThrow(InvalidSectionException())

        mockMvc.get("/api/v1/cms/unknown/categories")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_SECTION") }
                jsonPath("$.errorMessage") { isNotEmpty() }
            }
    }
}
