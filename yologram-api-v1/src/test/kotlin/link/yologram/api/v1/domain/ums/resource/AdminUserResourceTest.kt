package link.yologram.api.v1.domain.ums.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.AdminJwtProperties
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.ums.exception.AdminUserDuplicateException
import link.yologram.api.v1.domain.ums.exception.AdminUserNotFoundException
import link.yologram.api.v1.domain.ums.exception.AuthTokenExpiredException
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.exception.UmsExceptionHandler
import link.yologram.api.v1.domain.ums.model.AdminLoginRequest
import link.yologram.api.v1.domain.ums.model.AdminLoginResponse
import link.yologram.api.v1.domain.ums.model.AdminUserCreateRequest
import link.yologram.api.v1.domain.ums.model.AdminUserCreateResponse
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.service.AdminUserService
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import link.yologram.api.v1.global.exception.ValidationExceptionHandler
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(AdminUserResource::class)
@Import(
    UmsExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
    AuthenticatedAdminUserResolver::class,
)
class AdminUserResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var adminUserService: AdminUserService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    private fun createRequest(
        email: String = "new-admin@yologram.link",
        name: String = "새어드민",
        password: String = "password123",
    ) = AdminUserCreateRequest(email = email, name = name, password = password)

    @Nested
    inner class 어드민_생성 {

        @Nested
        inner class 성공 {

            @Test
            fun `유효한 어드민 토큰으로 생성 시 201과 uid를 반환한다`() {
                whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
                whenever(adminUserService.create(any())).thenReturn(AdminUserCreateResponse(uid = 2L))

                mockMvc.post("/api/v1/ums/admin/users") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(createRequest())
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.data.uid") { value(2) }
                }
            }
        }

        @Nested
        inner class 인증_실패 {

            @Test
            fun `Authorization 헤더가 없으면 401을 반환한다`() {
                mockMvc.post("/api/v1/ums/admin/users") {
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

                mockMvc.post("/api/v1/ums/admin/users") {
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

                mockMvc.post("/api/v1/ums/admin/users") {
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
            fun `중복 이메일이면 409를 반환한다`() {
                whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
                whenever(adminUserService.create(any())).thenThrow(AdminUserDuplicateException())

                mockMvc.post("/api/v1/ums/admin/users") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(createRequest())
                }.andExpect {
                    status { isConflict() }
                    jsonPath("$.errorCode") { value("ADMIN_USER_DUPLICATE") }
                }
            }

            @Test
            fun `이메일 형식이 아니면 400을 반환한다`() {
                whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)

                mockMvc.post("/api/v1/ums/admin/users") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"email":"invalid-email","name":"새어드민","password":"password123"}"""
                }.andExpect {
                    status { isBadRequest() }
                }
            }

            @Test
            fun `이름이 2자 미만이면 400을 반환한다`() {
                whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)

                mockMvc.post("/api/v1/ums/admin/users") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"email":"new-admin@yologram.link","name":"a","password":"password123"}"""
                }.andExpect {
                    status { isBadRequest() }
                }
            }

            @Test
            fun `비밀번호가 8자 미만이면 400을 반환한다`() {
                whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)

                mockMvc.post("/api/v1/ums/admin/users") {
                    header("Authorization", "Bearer admin-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"email":"new-admin@yologram.link","name":"새어드민","password":"short"}"""
                }.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }

    @Nested
    inner class 어드민_로그인 {

        @Nested
        inner class 성공 {

            @Test
            fun `로그인에 성공하면 200과 AdminLoginResponse를 반환한다`() {
                val response = AdminLoginResponse(1L, "admin-token", "admin@yologram.link", "어드민")
                whenever(adminUserService.login(any())).thenReturn(response)

                mockMvc.post("/api/v1/ums/admin/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(AdminLoginRequest("admin@yologram.link", "password123"))
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.uid") { value(1) }
                    jsonPath("$.data.accessToken") { value("admin-token") }
                    jsonPath("$.data.email") { value("admin@yologram.link") }
                    jsonPath("$.data.name") { value("어드민") }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `존재하지 않는 이메일이면 404를 반환한다`() {
                whenever(adminUserService.login(any())).thenThrow(AdminUserNotFoundException())

                mockMvc.post("/api/v1/ums/admin/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(AdminLoginRequest("notfound@yologram.link", "password123"))
                }.andExpect {
                    status { isNotFound() }
                    jsonPath("$.errorCode") { value("ADMIN_USER_NOT_FOUND") }
                }
            }

            @Test
            fun `비밀번호가 틀리면 401을 반환한다`() {
                whenever(adminUserService.login(any())).thenThrow(AuthWrongPasswordException())

                mockMvc.post("/api/v1/ums/admin/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(AdminLoginRequest("admin@yologram.link", "wrong"))
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_WRONG_PASSWORD") }
                }
            }

            @Test
            fun `이메일이 비어있으면 400을 반환한다`() {
                mockMvc.post("/api/v1/ums/admin/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"email":"","password":"password123"}"""
                }.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }
}
