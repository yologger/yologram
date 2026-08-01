package link.yologram.api.v1.domain.ums.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.AdminJwtProperties
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.ums.enum.UserStatus
import link.yologram.api.v1.domain.ums.exception.AdminUserDuplicateException
import link.yologram.api.v1.domain.ums.exception.AdminUserNotFoundException
import link.yologram.api.v1.domain.ums.exception.AdminUserSelfDeleteException
import link.yologram.api.v1.domain.ums.exception.AuthTokenExpiredException
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.exception.UmsExceptionHandler
import link.yologram.api.v1.domain.ums.model.AdminLoginRequest
import link.yologram.api.v1.domain.ums.model.AdminLoginResponse
import link.yologram.api.v1.domain.ums.model.AdminUserCreateRequest
import link.yologram.api.v1.domain.ums.model.AdminUserCreateResponse
import link.yologram.api.v1.domain.ums.model.AdminUserResponse
import link.yologram.api.v1.domain.ums.model.AdminValidateTokenResponse
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.service.AdminUserService
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import link.yologram.api.v1.global.exception.ValidationExceptionHandler
import link.yologram.api.v1.global.model.ApiEnvelopPage
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

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

                mockMvc.post("/api/v1/ums/admin/admin-users") {
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
                mockMvc.post("/api/v1/ums/admin/admin-users") {
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

                mockMvc.post("/api/v1/ums/admin/admin-users") {
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

                mockMvc.post("/api/v1/ums/admin/admin-users") {
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

                mockMvc.post("/api/v1/ums/admin/admin-users") {
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

                mockMvc.post("/api/v1/ums/admin/admin-users") {
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

                mockMvc.post("/api/v1/ums/admin/admin-users") {
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

                mockMvc.post("/api/v1/ums/admin/admin-users") {
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

    @Nested
    inner class 토큰_검증 {

        @Test
        fun `유효한 어드민 토큰이면 200과 어드민 정보를 반환한다`() {
            val response = AdminValidateTokenResponse(1L, "admin@yologram.link", "어드민")
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
            whenever(adminUserService.validateToken("admin-token")).thenReturn(response)

            mockMvc.post("/api/v1/ums/admin/auth/validate-token") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.uid") { value(1) }
                jsonPath("$.data.email") { value("admin@yologram.link") }
                jsonPath("$.data.name") { value("어드민") }
            }
        }

        @Test
        fun `만료된 어드민 토큰이면 401을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

            mockMvc.post("/api/v1/ums/admin/auth/validate-token") {
                header("Authorization", "Bearer expired-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
            }
        }

        @Test
        fun `유효하지 않은 어드민 토큰이면 401을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("invalid-token")).thenThrow(AuthTokenInvalidException())

            mockMvc.post("/api/v1/ums/admin/auth/validate-token") {
                header("Authorization", "Bearer invalid-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
        }

        @Test
        fun `Authorization 헤더가 없으면 401을 반환한다`() {
            mockMvc.post("/api/v1/ums/admin/auth/validate-token")
                .andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                }
        }

        @Test
        fun `존재하지 않는 어드민이면 404를 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(999L)
            whenever(adminUserService.validateToken("admin-token")).thenThrow(AdminUserNotFoundException())

            mockMvc.post("/api/v1/ums/admin/auth/validate-token") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("ADMIN_USER_NOT_FOUND") }
            }
        }
    }

    @Nested
    inner class 로그아웃 {

        @Test
        fun `유효한 어드민 토큰으로 로그아웃하면 204를 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)

            mockMvc.post("/api/v1/ums/admin/auth/logout") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isNoContent() }
            }
        }

        @Test
        fun `만료된 어드민 토큰으로 로그아웃하면 401을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

            mockMvc.post("/api/v1/ums/admin/auth/logout") {
                header("Authorization", "Bearer expired-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
            }
        }
    }

    @Nested
    inner class 어드민_목록_조회 {

        private fun adminUserResponse(uid: Long) = AdminUserResponse(
            uid = uid,
            email = "admin$uid@yologram.link",
            name = "어드민$uid",
            status = UserStatus.ACTIVE,
            joinedDate = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

        private fun pageResponse(
            data: List<AdminUserResponse>,
            page: Long = 0,
            size: Long = 10,
            totalPages: Long = 1,
            totalCount: Long = data.size.toLong(),
            first: Boolean = true,
            last: Boolean = true,
        ) = ApiEnvelopPage(
            data = data,
            page = page,
            size = size,
            totalPages = totalPages,
            totalCount = totalCount,
            first = first,
            last = last,
        )

        @Test
        fun `유효한 어드민 토큰으로 조회 시 200과 페이지 응답을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
            whenever(adminUserService.getAdminUsers(0, 2)).thenReturn(
                pageResponse(
                    data = listOf(adminUserResponse(1L), adminUserResponse(2L)),
                    page = 0,
                    size = 2,
                    totalPages = 3,
                    totalCount = 5,
                    first = true,
                    last = false,
                )
            )

            mockMvc.get("/api/v1/ums/admin/admin-users?page=0&size=2") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(2) }
                jsonPath("$.data[0].uid") { value(1) }
                jsonPath("$.data[0].email") { value("admin1@yologram.link") }
                jsonPath("$.data[0].name") { value("어드민1") }
                jsonPath("$.data[0].status") { value("ACTIVE") }
                jsonPath("$.data[0].joinedDate") { exists() }
                jsonPath("$.data[1].uid") { value(2) }
                jsonPath("$.page") { value(0) }
                jsonPath("$.size") { value(2) }
                jsonPath("$.totalPages") { value(3) }
                jsonPath("$.totalCount") { value(5) }
                jsonPath("$.first") { value(true) }
                jsonPath("$.last") { value(false) }
            }
        }

        @Test
        fun `page와 size 생략 시 기본값 0과 10으로 조회한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
            whenever(adminUserService.getAdminUsers(0, 10)).thenReturn(
                pageResponse(data = listOf(adminUserResponse(1L)))
            )

            mockMvc.get("/api/v1/ums/admin/admin-users") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.page") { value(0) }
                jsonPath("$.size") { value(10) }
            }
        }

        @Test
        fun `범위 밖 page면 빈 데이터 페이지를 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
            whenever(adminUserService.getAdminUsers(9, 10)).thenReturn(
                pageResponse(data = emptyList(), page = 9, totalPages = 1, totalCount = 2, first = false, last = true)
            )

            mockMvc.get("/api/v1/ums/admin/admin-users?page=9&size=10") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(0) }
                jsonPath("$.page") { value(9) }
                jsonPath("$.totalCount") { value(2) }
            }
        }

        @Test
        fun `size 1이면 200을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
            whenever(adminUserService.getAdminUsers(0, 1)).thenReturn(
                pageResponse(data = listOf(adminUserResponse(1L)), size = 1)
            )

            mockMvc.get("/api/v1/ums/admin/admin-users?size=1") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.size") { value(1) }
            }
        }

        @Test
        fun `size 100이면 200을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
            whenever(adminUserService.getAdminUsers(0, 100)).thenReturn(
                pageResponse(data = listOf(adminUserResponse(1L)), size = 100)
            )

            mockMvc.get("/api/v1/ums/admin/admin-users?size=100") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.size") { value(100) }
            }
        }

        @Test
        fun `page가 음수면 400을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)

            mockMvc.get("/api/v1/ums/admin/admin-users?page=-1") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `size가 0이면 400을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)

            mockMvc.get("/api/v1/ums/admin/admin-users?size=0") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `size가 101이면 400을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)

            mockMvc.get("/api/v1/ums/admin/admin-users?size=101") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `Authorization 헤더가 없으면 401을 반환한다`() {
            mockMvc.get("/api/v1/ums/admin/admin-users").andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
        }
    }

    @Nested
    inner class 어드민_삭제 {

        @Test
        fun `유효한 어드민 토큰으로 삭제하면 204를 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)

            mockMvc.delete("/api/v1/ums/admin/admin-users/2") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isNoContent() }
            }
        }

        @Test
        fun `자기 자신을 삭제하면 400을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
            whenever(adminUserService.delete(1L, 1L)).thenThrow(AdminUserSelfDeleteException())

            mockMvc.delete("/api/v1/ums/admin/admin-users/1") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("ADMIN_USER_SELF_DELETE") }
            }
        }

        @Test
        fun `없는 id면 404를 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("admin-token")).thenReturn(1L)
            whenever(adminUserService.delete(1L, 999L)).thenThrow(AdminUserNotFoundException())

            mockMvc.delete("/api/v1/ums/admin/admin-users/999") {
                header("Authorization", "Bearer admin-token")
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("ADMIN_USER_NOT_FOUND") }
            }
        }

        @Test
        fun `Authorization 헤더가 없으면 401을 반환한다`() {
            mockMvc.delete("/api/v1/ums/admin/admin-users/2").andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
        }

        @Test
        fun `만료된 어드민 토큰이면 401을 반환한다`() {
            whenever(adminJwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

            mockMvc.delete("/api/v1/ums/admin/admin-users/2") {
                header("Authorization", "Bearer expired-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
            }
        }
    }
}
