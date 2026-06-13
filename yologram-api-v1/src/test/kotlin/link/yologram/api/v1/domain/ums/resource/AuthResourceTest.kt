package link.yologram.api.v1.domain.ums.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.ums.exception.*
import link.yologram.api.v1.domain.ums.model.LoginRequest
import link.yologram.api.v1.domain.ums.model.LoginResponse
import link.yologram.api.v1.domain.ums.model.EmailVerificationSendRequest
import link.yologram.api.v1.domain.ums.model.ValidateTokenResponse
import link.yologram.api.v1.domain.ums.model.EmailVerificationVerifyRequest
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.service.AuthService
import link.yologram.api.v1.domain.ums.service.EmailVerificationService
import link.yologram.api.v1.domain.ums.util.JwtUtil
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(AuthResource::class)
@Import(AuthenticatedUserResolver::class)
class AuthResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var emailVerificationService: EmailVerificationService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @Nested
    inner class 로그인 {

        @Nested
        inner class 성공 {

            @Test
            fun `로그인에 성공하면 200과 LoginResponse를 반환한다`() {
                val response = LoginResponse(1L, "test-token", "test@yologram.link", "테스트", "tester")
                whenever(authService.login(any())).thenReturn(response)

                mockMvc.post("/api/v1/ums/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(LoginRequest("test@yologram.link", "password123!"))
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.uid") { value(1) }
                    jsonPath("$.data.accessToken") { value("test-token") }
                    jsonPath("$.data.email") { value("test@yologram.link") }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `존재하지 않는 이메일이면 404를 반환한다`() {
                whenever(authService.login(any())).thenThrow(UserNotFoundException())

                mockMvc.post("/api/v1/ums/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(LoginRequest("notfound@yologram.link", "password"))
                }.andExpect {
                    status { isNotFound() }
                    jsonPath("$.errorCode") { value("USER_NOT_FOUND") }
                }
            }

            @Test
            fun `비밀번호가 틀리면 401을 반환한다`() {
                whenever(authService.login(any())).thenThrow(AuthWrongPasswordException())

                mockMvc.post("/api/v1/ums/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(LoginRequest("test@yologram.link", "wrong"))
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_WRONG_PASSWORD") }
                }
            }

            @Test
            fun `이메일이 비어있으면 400을 반환한다`() {
                mockMvc.post("/api/v1/ums/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"email":"","password":"password123!"}"""
                }.andExpect {
                    status { isBadRequest() }
                }
            }
        }
    }

    @Nested
    inner class 토큰_검증 {

        @Test
        fun `유효한 토큰이면 200과 유저 정보를 반환한다`() {
            val response = ValidateTokenResponse(1L, "test@yologram.link", "테스트", "tester")
            whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
            whenever(authService.validateToken("valid-token")).thenReturn(response)

            mockMvc.post("/api/v1/ums/auth/validate-token") {
                header("Authorization", "Bearer valid-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.uid") { value(1) }
                jsonPath("$.data.email") { value("test@yologram.link") }
            }
        }

        @Test
        fun `만료된 토큰이면 401을 반환한다`() {
            whenever(jwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

            mockMvc.post("/api/v1/ums/auth/validate-token") {
                header("Authorization", "Bearer expired-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
            }
        }

        @Test
        fun `유효하지 않은 토큰이면 401을 반환한다`() {
            whenever(jwtUtil.validateAndGetUid("invalid-token")).thenThrow(AuthTokenInvalidException())

            mockMvc.post("/api/v1/ums/auth/validate-token") {
                header("Authorization", "Bearer invalid-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
        }

        @Test
        fun `Authorization 헤더가 없으면 401을 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/validate-token")
                .andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                }
        }

        @Test
        fun `Bearer 뒤에 토큰이 없으면 401을 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/validate-token") {
                header("Authorization", "Bearer ")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
        }

        @Test
        fun `Basic 스킴이면 401을 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/validate-token") {
                header("Authorization", "Basic dGVzdDp0ZXN0")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
        }
    }

    @Nested
    inner class 로그아웃 {

        @Test
        fun `유효한 토큰으로 로그아웃하면 204를 반환한다`() {
            whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

            mockMvc.post("/api/v1/ums/auth/logout") {
                header("Authorization", "Bearer valid-token")
            }.andExpect {
                status { isNoContent() }
            }
        }

        @Test
        fun `만료된 토큰으로 로그아웃하면 401을 반환한다`() {
            whenever(jwtUtil.validateAndGetUid("expired-token")).thenThrow(AuthTokenExpiredException())

            mockMvc.post("/api/v1/ums/auth/logout") {
                header("Authorization", "Bearer expired-token")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_EXPIRED_TOKEN") }
            }
        }
    }

    @Nested
    inner class 인증_코드_발송 {

        @Test
        fun `발송 성공 시 204를 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/email-verification/send") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(EmailVerificationSendRequest("test@yologram.link"))
            }.andExpect {
                status { isNoContent() }
            }
        }

        @Test
        fun `이미 가입된 이메일이면 409를 반환한다`() {
            whenever(emailVerificationService.sendCode("duplicate@yologram.link"))
                .thenThrow(UserDuplicateException())

            mockMvc.post("/api/v1/ums/auth/email-verification/send") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(EmailVerificationSendRequest("duplicate@yologram.link"))
            }.andExpect {
                status { isConflict() }
                jsonPath("$.errorCode") { value("USER_DUPLICATE") }
            }
        }

        @Test
        fun `이메일이 비어있으면 400을 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/email-verification/send") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":""}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `이메일 형식이 아니면 400을 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/email-verification/send") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"invalid-email"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    inner class 이메일_인증 {

        @Test
        fun `인증 성공 시 204를 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/email-verification/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(EmailVerificationVerifyRequest("test@yologram.link", "123456"))
            }.andExpect {
                status { isNoContent() }
            }
        }

        @Test
        fun `인증 코드가 만료되면 400을 반환한다`() {
            whenever(emailVerificationService.verifyCode("test@yologram.link", "123456"))
                .thenThrow(EmailVerificationExpiredException())

            mockMvc.post("/api/v1/ums/auth/email-verification/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(EmailVerificationVerifyRequest("test@yologram.link", "123456"))
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("EMAIL_VERIFICATION_EXPIRED") }
            }
        }

        @Test
        fun `인증 코드가 일치하지 않으면 400을 반환한다`() {
            whenever(emailVerificationService.verifyCode("test@yologram.link", "999999"))
                .thenThrow(EmailVerificationInvalidException())

            mockMvc.post("/api/v1/ums/auth/email-verification/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(EmailVerificationVerifyRequest("test@yologram.link", "999999"))
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("EMAIL_VERIFICATION_INVALID") }
            }
        }

        @Test
        fun `인증 코드가 6자리가 아니면 400을 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/email-verification/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"test@yologram.link","code":"123"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `이메일이 비어있으면 400을 반환한다`() {
            mockMvc.post("/api/v1/ums/auth/email-verification/verify") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"","code":"123456"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    inner class 리소스_없음 {

        @Test
        fun `존재하지 않는 경로는 404와 NOT_FOUND를 반환한다`() {
            mockMvc.get("/api/v1/ums/auth/not-exists").andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("NOT_FOUND") }
            }
        }
    }
}
