package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.model.LoginRequest
import link.yologram.api.v1.domain.ums.repository.UserRepository
import link.yologram.api.v1.domain.ums.util.JwtUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var passwordEncoder: BCryptPasswordEncoder

    @Mock
    lateinit var jwtUtil: JwtUtil

    @InjectMocks
    lateinit var authService: AuthService

    @Nested
    inner class 로그인 {

        @Nested
        inner class 성공 {

            @Test
            fun `이메일과 비밀번호가 일치하면 로그인에 성공한다`() {
                val user = User(
                    id = 1L,
                    email = "test@yologram.link",
                    name = "테스트",
                    nickname = "tester",
                    password = "encoded-password",
                )
                whenever(userRepository.findByEmail("test@yologram.link")).thenReturn(Optional.of(user))
                whenever(passwordEncoder.matches("password123!", "encoded-password")).thenReturn(true)
                whenever(jwtUtil.createToken(1L)).thenReturn("test-token")

                val result = authService.login(LoginRequest("test@yologram.link", "password123!"))

                assertEquals(1L, result.uid)
                assertEquals("test-token", result.accessToken)
                assertEquals("test@yologram.link", result.email)
                assertEquals("테스트", result.name)
                assertEquals("tester", result.nickname)
            }
        }

        @Nested
        inner class 실패 {

            @Test
            fun `존재하지 않는 이메일이면 UserNotFoundException을 던진다`() {
                whenever(userRepository.findByEmail(any())).thenReturn(Optional.empty())

                assertThrows<UserNotFoundException> {
                    authService.login(LoginRequest("notfound@yologram.link", "password"))
                }
            }

            @Test
            fun `비밀번호가 틀리면 AuthWrongPasswordException을 던진다`() {
                val user = User(
                    id = 1L,
                    email = "test@yologram.link",
                    name = "테스트",
                    nickname = "tester",
                    password = "encoded-password",
                )
                whenever(userRepository.findByEmail("test@yologram.link")).thenReturn(Optional.of(user))
                whenever(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false)

                assertThrows<AuthWrongPasswordException> {
                    authService.login(LoginRequest("test@yologram.link", "wrong-password"))
                }
            }
        }
    }

    @Nested
    inner class 토큰_검증 {

        @Test
        fun `DB에 저장된 토큰과 일치하면 유저 정보를 반환한다`() {
            val user = User(
                id = 1L,
                email = "test@yologram.link",
                name = "테스트",
                nickname = "tester",
                password = "encoded-password",
            )
            user.accessToken = "valid-token"
            whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            val result = authService.validateToken("valid-token")

            assertEquals(1L, result.uid)
            assertEquals("test@yologram.link", result.email)
        }

        @Test
        fun `로그아웃된 토큰이면 AuthTokenInvalidException을 던진다`() {
            val user = User(
                id = 1L,
                email = "test@yologram.link",
                name = "테스트",
                nickname = "tester",
                password = "encoded-password",
            )
            user.accessToken = null
            whenever(jwtUtil.validateAndGetUid("logged-out-token")).thenReturn(1L)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            assertThrows<AuthTokenInvalidException> {
                authService.validateToken("logged-out-token")
            }
        }
    }

    @Nested
    inner class 로그아웃 {

        @Test
        fun `로그아웃 시 accessToken을 null로 설정한다`() {
            val user = User(
                id = 1L,
                email = "test@yologram.link",
                name = "테스트",
                nickname = "tester",
                password = "encoded-password",
            )
            user.accessToken = "some-token"
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            authService.logout(1L)

            assertNull(user.accessToken)
        }

        @Test
        fun `존재하지 않는 유저면 UserNotFoundException을 던진다`() {
            whenever(userRepository.findById(any())).thenReturn(Optional.empty())

            assertThrows<UserNotFoundException> {
                authService.logout(999L)
            }
        }
    }
}
