package link.yologram.api.v1.domain.search.tech.resource

import link.yologram.api.v1.config.security.AdminJwtProperties
import link.yologram.api.v1.config.security.JwtProperties
import link.yologram.api.v1.domain.search.exception.InvalidIndexRangeException
import link.yologram.api.v1.domain.search.exception.SearchExceptionHandler
import link.yologram.api.v1.domain.search.tech.service.AdminTechNewsIndexingService
import link.yologram.api.v1.domain.ums.exception.AuthTokenExpiredException
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import link.yologram.api.v1.global.exception.ValidationExceptionHandler
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.put

@WebMvcTest(AdminTechNewsIndexingResource::class)
@Import(
    SearchExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
    AuthenticatedAdminUserResolver::class,
)
class AdminTechNewsIndexingResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var adminTechNewsIndexingService: AdminTechNewsIndexingService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    private val baseUrl = "/api/v1/search/admin/tech/news/indexing"

    private fun givenValidAdminToken() {
        whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
    }

    @Nested
    inner class 전체_인덱싱 {

        @Test
        fun `202를 반환하고 작업을 발행한다`() {
            givenValidAdminToken()

            mockMvc.put(baseUrl) {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isAccepted() }
            }

            verify(adminTechNewsIndexingService).fullIndexAsync()
        }

        @Test
        fun `토큰이 없으면 401이고 발행하지 않는다`() {
            mockMvc.put(baseUrl).andExpect {
                status { isUnauthorized() }
            }

            verify(adminTechNewsIndexingService, never()).fullIndexAsync()
        }

        @Test
        fun `토큰이 만료면 401`() {
            whenever(adminJwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

            mockMvc.put(baseUrl) {
                header("Authorization", "Bearer expired-token")
            }.andExpect {
                status { isUnauthorized() }
            }

            verify(adminTechNewsIndexingService, never()).fullIndexAsync()
        }

        @Test
        fun `토큰이 유효하지 않으면 401`() {
            whenever(adminJwtUtil.validateAndGetUid("bad-token")).thenThrow(AuthTokenInvalidException())

            mockMvc.put(baseUrl) {
                header("Authorization", "Bearer bad-token")
            }.andExpect {
                status { isUnauthorized() }
            }

            verify(adminTechNewsIndexingService, never()).fullIndexAsync()
        }
    }

    @Nested
    inner class 단건_인덱싱 {

        @Test
        fun `202를 반환하고 해당 id로 발행한다`() {
            givenValidAdminToken()

            mockMvc.put("$baseUrl/42") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isAccepted() }
            }

            verify(adminTechNewsIndexingService).index(eq(42L))
        }

        @Test
        fun `토큰이 없으면 401`() {
            mockMvc.put("$baseUrl/42").andExpect {
                status { isUnauthorized() }
            }

            verify(adminTechNewsIndexingService, never()).index(eq(42L))
        }

        @Test
        fun `id가 숫자가 아니면 400`() {
            givenValidAdminToken()

            mockMvc.put("$baseUrl/abc") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    inner class 범위_인덱싱 {

        @Test
        fun `202를 반환하고 범위로 발행한다`() {
            givenValidAdminToken()

            mockMvc.put("$baseUrl/1/45") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isAccepted() }
            }

            verify(adminTechNewsIndexingService).index(from = eq(1L), to = eq(45L))
        }

        @Test
        fun `from이 to보다 크면 400`() {
            givenValidAdminToken()
            whenever(adminTechNewsIndexingService.index(from = eq(30L), to = eq(10L)))
                .thenThrow(InvalidIndexRangeException())

            mockMvc.put("$baseUrl/30/10") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_INDEX_RANGE") }
            }
        }

        @Test
        fun `토큰이 없으면 401`() {
            mockMvc.put("$baseUrl/1/45").andExpect {
                status { isUnauthorized() }
            }

            verify(adminTechNewsIndexingService, never()).index(from = eq(1L), to = eq(45L))
        }
    }
}
