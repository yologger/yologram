package link.yologram.api.v1.domain.ums.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.ums.enum.UserType
import link.yologram.api.v1.domain.ums.exception.UmsExceptionHandler
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.model.ChangePasswordRequest
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.model.JoinResponse
import link.yologram.api.v1.domain.ums.model.UpdateProfileRequest
import link.yologram.api.v1.domain.ums.model.UserMeResponse
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
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

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

    @Nested
    inner class 회원정보_조회 {

        private val meResponse = UserMeResponse(
            uid = 1L,
            email = "test@yologram.link",
            name = "테스터",
            nickname = "tester",
            avatar = null,
            type = UserType.DEFAULT,
            joinedDate = LocalDateTime.of(2025, 1, 1, 0, 0),
        )

        @Nested
        inner class 성공 {

            @Test
            fun `200과 유저 정보를 반환한다`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
                whenever(userService.getMe(1L)).thenReturn(meResponse)

                mockMvc.get("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer valid-token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.uid") { value(1) }
                    jsonPath("$.data.email") { value("test@yologram.link") }
                    jsonPath("$.data.name") { value("테스터") }
                    jsonPath("$.data.nickname") { value("tester") }
                    jsonPath("$.data.avatar") { doesNotExist() }
                    jsonPath("$.data.type") { value("DEFAULT") }
                    jsonPath("$.data.joinedDate") { isNotEmpty() }
                }
            }

            @Test
            fun `아바타가 있으면 포함된다`() {
                val withAvatar = meResponse.copy(avatar = "https://example.com/avatar.png")
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
                whenever(userService.getMe(1L)).thenReturn(withAvatar)

                mockMvc.get("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer valid-token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.avatar") { value("https://example.com/avatar.png") }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `Authorization 헤더 없으면 401 반환`() {
                mockMvc.get("/api/v1/ums/user/me")
                    .andExpect {
                        status { isUnauthorized() }
                        jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                    }
            }

            @Test
            fun `유효하지 않은 토큰이면 401 반환`() {
                whenever(jwtUtil.validateAndGetUid("invalid-token"))
                    .thenThrow(link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException())

                mockMvc.get("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer invalid-token")
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                }
            }

            @Test
            fun `존재하지 않는 유저면 404 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(999L)
                whenever(userService.getMe(999L)).thenThrow(UserNotFoundException())

                mockMvc.get("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer valid-token")
                }.andExpect {
                    status { isNotFound() }
                    jsonPath("$.errorCode") { value("USER_NOT_FOUND") }
                }
            }
        }
    }

    @Nested
    inner class 회원정보_수정 {

        private val updatedResponse = UserMeResponse(
            uid = 1L,
            email = "test@yologram.link",
            name = "테스터",
            nickname = "new-nickname",
            avatar = null,
            type = UserType.DEFAULT,
            joinedDate = LocalDateTime.of(2025, 1, 1, 0, 0),
        )

        @Nested
        inner class 성공 {

            @Test
            fun `200과 수정된 유저 정보를 반환한다`() {
                val request = UpdateProfileRequest(nickname = "new-nickname")
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
                whenever(userService.updateProfile(any(), any())).thenReturn(updatedResponse)

                mockMvc.patch("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.uid") { value(1) }
                    jsonPath("$.data.nickname") { value("new-nickname") }
                    jsonPath("$.data.email") { value("test@yologram.link") }
                    jsonPath("$.data.name") { value("테스터") }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `Authorization 헤더 없으면 401 반환`() {
                mockMvc.patch("/api/v1/ums/user/me") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(UpdateProfileRequest(nickname = "new-nickname"))
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                }
            }

            @Test
            fun `존재하지 않는 유저면 404 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(999L)
                doThrow(UserNotFoundException()).whenever(userService).updateProfile(any(), any())

                mockMvc.patch("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(UpdateProfileRequest(nickname = "new-nickname"))
                }.andExpect {
                    status { isNotFound() }
                    jsonPath("$.errorCode") { value("USER_NOT_FOUND") }
                }
            }

            @Test
            fun `닉네임 누락 시 400 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

                mockMvc.patch("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = "{}"
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `닉네임 1자 시 400 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

                mockMvc.patch("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(UpdateProfileRequest(nickname = "a"))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `닉네임 21자 시 400 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

                mockMvc.patch("/api/v1/ums/user/me") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(UpdateProfileRequest(nickname = "a".repeat(21)))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }
        }
    }

    private fun changePasswordRequest(
        currentPassword: String = "password123",
        newPassword: String = "newpass1234",
    ) = ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)

    @Nested
    inner class 비밀번호_변경 {

        @Nested
        inner class 성공 {

            @Test
            fun `204 반환`() {
                val request = changePasswordRequest()
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
                doNothing().whenever(userService).changePassword(any(), any())

                mockMvc.patch("/api/v1/ums/user/me/password") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isNoContent() }
                }
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `Authorization 헤더 없으면 401 반환`() {
                mockMvc.patch("/api/v1/ums/user/me/password") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(changePasswordRequest())
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
                }
            }

            @Test
            fun `현재 비밀번호 불일치 시 401 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
                doThrow(AuthWrongPasswordException()).whenever(userService).changePassword(any(), any())

                mockMvc.patch("/api/v1/ums/user/me/password") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(changePasswordRequest())
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.errorCode") { value("AUTH_WRONG_PASSWORD") }
                }
            }

            @Test
            fun `존재하지 않는 유저면 404 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(999L)
                doThrow(UserNotFoundException()).whenever(userService).changePassword(any(), any())

                mockMvc.patch("/api/v1/ums/user/me/password") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(changePasswordRequest())
                }.andExpect {
                    status { isNotFound() }
                    jsonPath("$.errorCode") { value("USER_NOT_FOUND") }
                }
            }

            @Test
            fun `현재 비밀번호 누락 시 400 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

                mockMvc.patch("/api/v1/ums/user/me/password") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(mapOf("newPassword" to "newpass1234"))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `새 비밀번호 누락 시 400 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

                mockMvc.patch("/api/v1/ums/user/me/password") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(mapOf("currentPassword" to "password123"))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }

            @Test
            fun `새 비밀번호 7자 시 400 반환`() {
                whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

                mockMvc.patch("/api/v1/ums/user/me/password") {
                    header("Authorization", "Bearer valid-token")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(changePasswordRequest(newPassword = "1234567"))
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
                }
            }
        }
    }
}
