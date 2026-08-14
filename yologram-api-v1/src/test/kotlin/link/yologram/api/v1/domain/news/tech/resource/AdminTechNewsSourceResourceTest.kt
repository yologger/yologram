package link.yologram.api.v1.domain.news.tech.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.security.AdminJwtProperties
import link.yologram.api.v1.config.security.JwtProperties
import link.yologram.api.v1.domain.news.tech.exception.TechNewsExceptionHandler
import link.yologram.api.v1.domain.news.tech.exception.TechNewsSourceDuplicateException
import link.yologram.api.v1.domain.news.tech.exception.TechNewsSourceNotFoundException
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceCreateRequest
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceResponse
import link.yologram.api.v1.domain.news.tech.service.AdminTechNewsSourceService
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@WebMvcTest(AdminTechNewsSourceResource::class)
@Import(
    TechNewsExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
    AuthenticatedAdminUserResolver::class,
)
class AdminTechNewsSourceResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var adminTechNewsSourceService: AdminTechNewsSourceService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    private val baseUrl = "/api/v1/news/admin/tech/sources"

    private fun givenValidAdminToken() {
        whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
    }

    private fun createRequest(
        name: String = "GeekNews",
        url: String = "https://news.hada.io/rss/news",
        isActive: Boolean = true,
    ) = AdminTechNewsSourceCreateRequest(name = name, url = url, isActive = isActive)

    private fun testResponse(
        id: Long = 1L,
        name: String = "GeekNews",
        url: String = "https://news.hada.io/rss/news",
        isActive: Boolean = true,
    ) = AdminTechNewsSourceResponse(
        id = id,
        name = name,
        url = url,
        isActive = isActive,
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        modifiedDate = LocalDateTime.of(2026, 1, 2, 0, 0),
    )

    @Nested
    inner class 목록_조회 {

        @Test
        fun `유효한 어드민 토큰으로 조회 시 200과 소스 목록을 반환한다`() {
            givenValidAdminToken()
            whenever(adminTechNewsSourceService.getSources()).thenReturn(
                listOf(
                    testResponse(id = 1L, name = "소스1", url = "https://example.com/rss/1"),
                    testResponse(id = 2L, name = "소스2", url = "https://example.com/rss/2", isActive = false),
                )
            )

            mockMvc.get(baseUrl) {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(2) }
                jsonPath("$.data[0].id") { value(1) }
                jsonPath("$.data[0].name") { value("소스1") }
                jsonPath("$.data[0].url") { value("https://example.com/rss/1") }
                jsonPath("$.data[0].isActive") { value(true) }
                jsonPath("$.data[0].createdAt") { exists() }
                jsonPath("$.data[0].modifiedDate") { exists() }
                jsonPath("$.data[1].id") { value(2) }
                jsonPath("$.data[1].isActive") { value(false) }
            }
        }

        @Test
        fun `소스가 없으면 빈 배열을 반환한다`() {
            givenValidAdminToken()
            whenever(adminTechNewsSourceService.getSources()).thenReturn(emptyList())

            mockMvc.get(baseUrl) {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(0) }
            }
        }

        @Test
        fun `Authorization 헤더가 없으면 401을 반환한다`() {
            mockMvc.get(baseUrl).andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
        }

        @Test
        fun `만료된 어드민 토큰이면 401을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

            mockMvc.get(baseUrl) {
                header("Authorization", "Bearer expired-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
            }
        }
    }

    @Nested
    inner class 생성 {

        @Nested
        inner class 성공 {

            @Test
            fun `유효한 요청이면 201과 생성된 소스를 반환한다`() {
                givenValidAdminToken()
                whenever(adminTechNewsSourceService.create(any())).thenReturn(testResponse())

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(createRequest())
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.data.id") { value(1) }
                    jsonPath("$.data.name") { value("GeekNews") }
                    jsonPath("$.data.url") { value("https://news.hada.io/rss/news") }
                    jsonPath("$.data.isActive") { value(true) }
                    jsonPath("$.data.createdAt") { exists() }
                    jsonPath("$.data.modifiedDate") { exists() }
                }
            }

            @Test
            fun `isActive 생략 시에도 201을 반환한다`() {
                givenValidAdminToken()
                whenever(adminTechNewsSourceService.create(any())).thenReturn(testResponse())

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"GeekNews","url":"https://news.hada.io/rss/news"}"""
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.data.isActive") { value(true) }
                }
            }
        }

        @Nested
        inner class 인증_실패 {

            @Test
            fun `Authorization 헤더가 없으면 401을 반환한다`() {
                mockMvc.post(baseUrl) {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(createRequest())
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                }
            }

            @Test
            fun `만료된 어드민 토큰이면 401을 반환한다`() {
                whenever(adminJwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer expired-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(createRequest())
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
                }
            }

            @Test
            fun `유효하지 않은 어드민 토큰이면 401을 반환한다`() {
                whenever(adminJwtUtil.validateAndGetUid("invalid-token")).thenThrow(AuthTokenInvalidException())

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer invalid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(createRequest())
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `name이 공백이면 400을 반환한다`() {
                givenValidAdminToken()

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"   ","url":"https://news.hada.io/rss/news"}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `name이 100자를 초과하면 400을 반환한다`() {
                givenValidAdminToken()
                val longName = "a".repeat(101)

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"$longName","url":"https://news.hada.io/rss/news"}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `url이 http나 https 형식이 아니면 400을 반환한다`() {
                givenValidAdminToken()

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"GeekNews","url":"ftp://news.hada.io/rss"}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `url이 501자면 400을 반환한다`() {
                givenValidAdminToken()
                // "https://example.com/"(20자) + a*481 = 501자
                val longUrl = "https://example.com/" + "a".repeat(481)

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"GeekNews","url":"$longUrl"}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `url 중복이면 409를 반환한다`() {
                givenValidAdminToken()
                whenever(adminTechNewsSourceService.create(any())).thenThrow(TechNewsSourceDuplicateException())

                mockMvc.post(baseUrl) {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(createRequest())
                }.andExpect {
                    status { isConflict() }
                    jsonPath("$.errorCode") { value("NEWS_SOURCE_DUPLICATE") }
                }
            }
        }
    }

    @Nested
    inner class 수정 {

        @Nested
        inner class 성공 {

            @Test
            fun `유효한 요청이면 200과 수정된 소스를 반환한다`() {
                givenValidAdminToken()
                whenever(adminTechNewsSourceService.update(eq(1L), any())).thenReturn(
                    testResponse(name = "새이름", isActive = false)
                )

                mockMvc.patch("$baseUrl/1") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"새이름","isActive":false}"""
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.id") { value(1) }
                    jsonPath("$.data.name") { value("새이름") }
                    jsonPath("$.data.isActive") { value(false) }
                }
            }

            @Test
            fun `빈 본문(전 필드 생략)이어도 200을 반환한다`() {
                givenValidAdminToken()
                whenever(adminTechNewsSourceService.update(eq(1L), any())).thenReturn(testResponse())

                mockMvc.patch("$baseUrl/1") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = "{}"
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.id") { value(1) }
                }
            }
        }

        @Nested
        inner class 인증_실패 {

            @Test
            fun `Authorization 헤더가 없으면 401을 반환한다`() {
                mockMvc.patch("$baseUrl/1") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"새이름"}"""
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                }
            }

            @Test
            fun `만료된 어드민 토큰이면 401을 반환한다`() {
                whenever(adminJwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

                mockMvc.patch("$baseUrl/1") {
                    header("Authorization", "Bearer expired-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"새이름"}"""
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `없는 id면 404를 반환한다`() {
                givenValidAdminToken()
                whenever(adminTechNewsSourceService.update(eq(999L), any())).thenThrow(TechNewsSourceNotFoundException())

                mockMvc.patch("$baseUrl/999") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"새이름"}"""
                }.andExpect {
                    status { isNotFound() }
                    jsonPath("$.errorCode") { value("NEWS_SOURCE_NOT_FOUND") }
                }
            }

            @Test
            fun `다른 소스와 url이 중복되면 409를 반환한다`() {
                givenValidAdminToken()
                whenever(adminTechNewsSourceService.update(eq(1L), any())).thenThrow(TechNewsSourceDuplicateException())

                mockMvc.patch("$baseUrl/1") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"url":"https://example.com/rss/dup"}"""
                }.andExpect {
                    status { isConflict() }
                    jsonPath("$.errorCode") { value("NEWS_SOURCE_DUPLICATE") }
                }
            }

            @Test
            fun `name이 공백이면 400을 반환한다`() {
                givenValidAdminToken()

                mockMvc.patch("$baseUrl/1") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"name":"   "}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `url이 http나 https 형식이 아니면 400을 반환한다`() {
                givenValidAdminToken()

                mockMvc.patch("$baseUrl/1") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"url":"not-a-url"}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `url이 501자면 400을 반환한다`() {
                givenValidAdminToken()
                val longUrl = "https://example.com/" + "a".repeat(481)

                mockMvc.patch("$baseUrl/1") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"url":"$longUrl"}"""
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }
        }
    }

    @Nested
    inner class 삭제 {

        @Test
        fun `유효한 어드민 토큰으로 삭제하면 204를 반환한다`() {
            givenValidAdminToken()

            mockMvc.delete("$baseUrl/1") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isNoContent() }
            }
        }

        @Test
        fun `없는 id면 404를 반환한다`() {
            givenValidAdminToken()
            whenever(adminTechNewsSourceService.delete(999L)).thenThrow(TechNewsSourceNotFoundException())

            mockMvc.delete("$baseUrl/999") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("NEWS_SOURCE_NOT_FOUND") }
            }
        }

        @Test
        fun `Authorization 헤더가 없으면 401을 반환한다`() {
            mockMvc.delete("$baseUrl/1").andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
        }

        @Test
        fun `만료된 어드민 토큰이면 401을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

            mockMvc.delete("$baseUrl/1") {
                header("Authorization", "Bearer expired-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
            }
        }
    }
}
