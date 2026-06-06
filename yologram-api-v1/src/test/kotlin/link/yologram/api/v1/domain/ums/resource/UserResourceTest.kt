package link.yologram.api.v1.domain.ums.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.ums.exception.UmsExceptionHandler
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.model.JoinResponse
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.service.UserService
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

@WebMvcTest(UserResource::class)
@Import(UmsExceptionHandler::class, ValidationExceptionHandler::class, GlobalExceptionHandler::class, AuthenticatedUserResolver::class)
class UserResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var userService: UserService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    private fun joinRequest(
        email: String = "test@yologram.link",
        name: String = "테스터",
        nickname: String = "tester",
        password: String = "password123",
    ) = JoinRequest(email = email, name = name, nickname = nickname, password = password)

    @Nested
    inner class 회원가입_성공 {

        @Test
        fun `201 반환`() {
            whenever(userService.join(any())).thenReturn(JoinResponse(uid = 1))

            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(joinRequest())
            }.andExpect {
                status { isCreated() }
                jsonPath("$.data.uid") { value(1) }
            }
        }
    }

    @Nested
    inner class 회원가입_중복 {

        @Test
        fun `중복 이메일 시 409 반환`() {
            whenever(userService.join(any())).thenThrow(UserDuplicateException())

            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(joinRequest())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.errorCode") { value("USER_DUPLICATE") }
                jsonPath("$.errorMessage") { isNotEmpty() }
            }
        }
    }

    @Nested
    inner class 회원가입_입력값_검증 {

        @Test
        fun `이메일 누락 시 400 반환`() {
            val body = mapOf("name" to "테스터", "nickname" to "tester", "password" to "password123")

            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `잘못된 이메일 형식 시 400 반환`() {
            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(joinRequest(email = "invalid-email"))
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `이름 누락 시 400 반환`() {
            val body = mapOf("email" to "test@yologram.link", "nickname" to "tester", "password" to "password123")

            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `닉네임 누락 시 400 반환`() {
            val body = mapOf("email" to "test@yologram.link", "name" to "테스터", "password" to "password123")

            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `비밀번호 누락 시 400 반환`() {
            val body = mapOf("email" to "test@yologram.link", "name" to "테스터", "nickname" to "tester")

            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `이름 1자 시 400 반환`() {
            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(joinRequest(name = "a"))
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `닉네임 1자 시 400 반환`() {
            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(joinRequest(nickname = "a"))
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `비밀번호 7자 시 400 반환`() {
            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(joinRequest(password = "1234567"))
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }

        @Test
        fun `빈 body 시 400 반환`() {
            mockMvc.post("/api/v1/ums/user/join") {
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
            }
        }
    }
}
