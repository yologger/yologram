package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.enum.UserStatus
import link.yologram.api.v1.domain.ums.enum.UserType
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var passwordEncoder: BCryptPasswordEncoder

    @InjectMocks
    lateinit var userService: UserService

    private fun joinRequest(
        email: String = "test@yologram.link",
        name: String = "테스터",
        nickname: String = "tester",
        password: String = "password123",
    ) = JoinRequest(email = email, name = name, nickname = nickname, password = password)

    @Nested
    inner class 회원가입_성공 {

        @Test
        fun `uid를 반환한다`() {
            val request = joinRequest()

            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(userRepository.save(any<User>())).thenReturn(
                User(id = 1, email = request.email, name = request.name, nickname = request.nickname, password = "encoded-password")
            )

            val response = userService.join(request)

            assertEquals(1L, response.uid)
        }

        @Test
        fun `비밀번호가 암호화된다`() {
            val request = joinRequest()

            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }

            userService.join(request)

            verify(passwordEncoder).encode(request.password)
        }

        @Test
        fun `기본 타입은 DEFAULT, 상태는 ACTIVE`() {
            val request = joinRequest()

            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(userRepository.save(any<User>())).thenAnswer {
                val user = it.arguments[0] as User
                assertEquals(UserType.DEFAULT, user.type)
                assertEquals(UserStatus.ACTIVE, user.status)
                user
            }

            userService.join(request)
        }
    }

    @Nested
    inner class 회원가입_실패 {

        @Test
        fun `중복 이메일 시 UserDuplicateException 발생`() {
            val request = joinRequest()

            whenever(userRepository.existsByEmail(request.email)).thenReturn(true)

            val exception = assertThrows<UserDuplicateException> {
                userService.join(request)
            }

            assertEquals("USER_DUPLICATE", exception.errorCode)
        }
    }
}
