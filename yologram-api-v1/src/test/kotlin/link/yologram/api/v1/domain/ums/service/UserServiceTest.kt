package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.UserEmailVerification
import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.enum.UserStatus
import link.yologram.api.v1.domain.ums.enum.UserType
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.exception.UserEmailNotVerifiedException
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.model.ChangePasswordRequest
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.model.UpdateProfileRequest
import link.yologram.api.v1.domain.ums.repository.UserEmailVerificationRepository
import link.yologram.api.v1.domain.ums.repository.UserRepository
import link.yologram.api.v1.infra.cache.Cache
import link.yologram.api.v1.infra.cache.CacheService
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var emailVerificationCodeRepository: UserEmailVerificationRepository

    @Mock
    lateinit var passwordEncoder: BCryptPasswordEncoder

    @Mock
    lateinit var cacheService: CacheService

    @InjectMocks
    lateinit var userService: UserService

    private fun joinRequest(
        email: String = "test@yologram.link",
        name: String = "테스터",
        nickname: String = "tester",
        password: String = "password123",
    ) = JoinRequest(email = email, name = name, nickname = nickname, password = password)

    private fun verifiedUserEmailVerification(email: String) = UserEmailVerification(
        id = 1L,
        email = email,
        code = "123456",
        verified = true,
        expiredAt = LocalDateTime.now().plusMinutes(5),
    )

    @Nested
    inner class 회원가입_성공 {

        @Test
        fun `uid를 반환한다`() {
            val request = joinRequest()

            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(request.email))
                .thenReturn(Optional.of(verifiedUserEmailVerification(request.email)))
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
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(request.email))
                .thenReturn(Optional.of(verifiedUserEmailVerification(request.email)))
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }

            userService.join(request)

            verify(passwordEncoder).encode(request.password)
        }

        @Test
        fun `기본 타입은 DEFAULT, 상태는 ACTIVE`() {
            val request = joinRequest()

            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(request.email))
                .thenReturn(Optional.of(verifiedUserEmailVerification(request.email)))
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(userRepository.save(any<User>())).thenAnswer {
                val user = it.arguments[0] as User
                assertEquals(UserType.DEFAULT, user.type)
                assertEquals(UserStatus.ACTIVE, user.status)
                user
            }

            userService.join(request)
        }

        @Test
        fun `가입 완료 후 인증 레코드를 삭제한다`() {
            val request = joinRequest()

            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(request.email))
                .thenReturn(Optional.of(verifiedUserEmailVerification(request.email)))
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }

            userService.join(request)

            verify(emailVerificationCodeRepository).deleteAllByEmail(request.email)
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

        @Test
        fun `이메일 인증 레코드가 없으면 UserEmailNotVerifiedException 발생`() {
            val request = joinRequest()

            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(request.email))
                .thenReturn(Optional.empty())

            assertThrows<UserEmailNotVerifiedException> {
                userService.join(request)
            }
        }

        @Test
        fun `이메일 인증이 미완료면 UserEmailNotVerifiedException 발생`() {
            val request = joinRequest()
            val unverified = UserEmailVerification(
                id = 1L,
                email = request.email,
                code = "123456",
                verified = false,
                expiredAt = LocalDateTime.now().plusMinutes(5),
            )

            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(request.email))
                .thenReturn(Optional.of(unverified))

            assertThrows<UserEmailNotVerifiedException> {
                userService.join(request)
            }
        }
    }

    private fun testUser(
        id: Long = 1L,
        email: String = "test@yologram.link",
        name: String = "테스터",
        nickname: String = "tester",
    ) = User(id = id, email = email, name = name, nickname = nickname, password = "encoded-password")

    @Nested
    inner class 회원정보_조회_성공 {

        @Test
        fun `유저 정보를 반환한다`() {
            val user = testUser()
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            val response = userService.getMe(1L)

            assertEquals(1L, response.uid)
            assertEquals("test@yologram.link", response.email)
            assertEquals("테스터", response.name)
            assertEquals("tester", response.nickname)
            assertNull(response.avatar)
            assertEquals(UserType.DEFAULT, response.type)
        }

        @Test
        fun `아바타가 있으면 포함된다`() {
            val user = testUser().apply { avatar = "https://example.com/avatar.png" }
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            val response = userService.getMe(1L)

            assertEquals("https://example.com/avatar.png", response.avatar)
        }
    }

    @Nested
    inner class 회원정보_조회_실패 {

        @Test
        fun `존재하지 않는 유저 시 UserNotFoundException 발생`() {
            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            val exception = assertThrows<UserNotFoundException> {
                userService.getMe(999L)
            }

            assertEquals("USER_NOT_FOUND", exception.errorCode)
        }
    }

    @Nested
    inner class 회원정보_수정_성공 {

        @Test
        fun `닉네임이 변경된다`() {
            val user = testUser()
            val request = UpdateProfileRequest(nickname = "new-nickname")

            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            val response = userService.updateProfile(1L, request)

            assertEquals("new-nickname", user.nickname)
            assertEquals("new-nickname", response.nickname)
        }

        @Test
        fun `변경된 유저 정보를 반환한다`() {
            val user = testUser()
            val request = UpdateProfileRequest(nickname = "new-nickname")

            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            val response = userService.updateProfile(1L, request)

            assertEquals(1L, response.uid)
            assertEquals("test@yologram.link", response.email)
            assertEquals("테스터", response.name)
            assertEquals("new-nickname", response.nickname)
        }

        @Test
        fun `닉네임 캐시를 무효화한다`() {
            val user = testUser()
            val request = UpdateProfileRequest(nickname = "new-nickname")

            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            userService.updateProfile(1L, request)

            // Cache는 TypeReference를 담아 인스턴스 동등성 비교가 안 되므로 key로 검증
            val captor = argumentCaptor<Cache<*>>()
            verify(cacheService).deleteAll(captor.capture())
            assertEquals(Cache.userNickname(1L).key, captor.firstValue.key)
        }
    }

    @Nested
    inner class 회원정보_수정_실패 {

        @Test
        fun `존재하지 않는 유저 시 UserNotFoundException 발생`() {
            val request = UpdateProfileRequest(nickname = "new-nickname")

            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            assertThrows<UserNotFoundException> {
                userService.updateProfile(999L, request)
            }
        }
    }

    @Nested
    inner class 비밀번호_변경_성공 {

        @Test
        fun `비밀번호가 변경된다`() {
            val user = testUser()
            val request = ChangePasswordRequest(currentPassword = "password123", newPassword = "newpass1234")

            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true)
            whenever(passwordEncoder.encode("newpass1234")).thenReturn("new-encoded-password")

            userService.changePassword(1L, request)

            assertEquals("new-encoded-password", user.password)
        }
    }

    @Nested
    inner class 비밀번호_변경_실패 {

        @Test
        fun `현재 비밀번호 불일치 시 AuthWrongPasswordException 발생`() {
            val user = testUser()
            val request = ChangePasswordRequest(currentPassword = "wrongpass", newPassword = "newpass1234")

            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false)

            val exception = assertThrows<AuthWrongPasswordException> {
                userService.changePassword(1L, request)
            }

            assertEquals("AUTH_WRONG_PASSWORD", exception.errorCode)
        }

        @Test
        fun `존재하지 않는 유저 시 UserNotFoundException 발생`() {
            val request = ChangePasswordRequest(currentPassword = "password123", newPassword = "newpass1234")

            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            assertThrows<UserNotFoundException> {
                userService.changePassword(999L, request)
            }
        }
    }

    @Nested
    inner class 회원탈퇴 {

        @Test
        fun `유저 레코드를 하드 삭제한다`() {
            val user = testUser()
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            userService.withdraw(1L)

            verify(userRepository).delete(user)
        }

        @Test
        fun `닉네임 캐시를 무효화한다`() {
            val user = testUser()
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            userService.withdraw(1L)

            val captor = argumentCaptor<Cache<*>>()
            verify(cacheService).deleteAll(captor.capture())
            assertEquals(Cache.userNickname(1L).key, captor.firstValue.key)
        }

        @Test
        fun `존재하지 않는 유저 시 UserNotFoundException 발생`() {
            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            assertThrows<UserNotFoundException> {
                userService.withdraw(999L)
            }
        }
    }
}
