package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.UserPasswordResetCode
import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.exception.UserPasswordResetExpiredException
import link.yologram.api.v1.domain.ums.exception.UserPasswordResetInvalidException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.repository.UserPasswordResetCodeRepository
import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserPasswordResetServiceTest {

    @Mock
    lateinit var passwordResetCodeRepository: UserPasswordResetCodeRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var emailSender: EmailSender

    @Mock
    lateinit var passwordEncoder: BCryptPasswordEncoder

    @InjectMocks
    lateinit var passwordResetService: UserPasswordResetService

    private fun resetCode(code: String = "123456", expiredAt: LocalDateTime = LocalDateTime.now().plusMinutes(5)) =
        UserPasswordResetCode(id = 1L, email = "test@yologram.link", code = code, expiredAt = expiredAt)

    @Nested
    inner class 코드_발송 {

        @Test
        fun `가입된 이메일이면 코드를 발송한다`() {
            whenever(userRepository.existsByEmail("test@yologram.link")).thenReturn(true)
            whenever(passwordResetCodeRepository.save(any<UserPasswordResetCode>())).thenAnswer { it.arguments[0] }

            passwordResetService.sendCode("test@yologram.link")

            verify(passwordResetCodeRepository).deleteAllByEmail("test@yologram.link")
            verify(passwordResetCodeRepository).save(argThat<UserPasswordResetCode> {
                email == "test@yologram.link" && code.length == 6
            })
            verify(emailSender).sendUserPasswordResetCode(eq("test@yologram.link"), any())
        }

        @Test
        fun `가입되지 않은 이메일이면 UserNotFoundException을 던진다`() {
            whenever(userRepository.existsByEmail("unknown@yologram.link")).thenReturn(false)

            assertThrows<UserNotFoundException> {
                passwordResetService.sendCode("unknown@yologram.link")
            }

            verify(emailSender, never()).sendUserPasswordResetCode(any(), any())
        }
    }

    @Nested
    inner class 코드_검증 {

        @Test
        fun `올바른 코드면 verified를 true로 만든다`() {
            val code = resetCode()
            whenever(passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(code))

            passwordResetService.verifyCode("test@yologram.link", "123456")

            assertTrue(code.verified)
        }

        @Test
        fun `코드 레코드가 없으면 UserPasswordResetInvalidException을 던진다`() {
            whenever(passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.empty())

            assertThrows<UserPasswordResetInvalidException> {
                passwordResetService.verifyCode("test@yologram.link", "123456")
            }
        }

        @Test
        fun `코드가 만료되면 UserPasswordResetExpiredException을 던진다`() {
            whenever(passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(resetCode(expiredAt = LocalDateTime.now().minusMinutes(1))))

            assertThrows<UserPasswordResetExpiredException> {
                passwordResetService.verifyCode("test@yologram.link", "123456")
            }
        }

        @Test
        fun `코드가 일치하지 않으면 UserPasswordResetInvalidException을 던진다`() {
            whenever(passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(resetCode()))

            assertThrows<UserPasswordResetInvalidException> {
                passwordResetService.verifyCode("test@yologram.link", "999999")
            }
        }
    }

    @Nested
    inner class 비밀번호_변경 {

        @Test
        fun `코드 재검증 후 비밀번호를 변경하고 코드를 삭제한다`() {
            val user = User(email = "test@yologram.link", name = "테스터", nickname = "tester", password = "old")
            whenever(passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(resetCode()))
            whenever(userRepository.findByEmail("test@yologram.link")).thenReturn(Optional.of(user))
            whenever(passwordEncoder.encode("newpass1234")).thenReturn("encoded")

            passwordResetService.confirm("test@yologram.link", "123456", "newpass1234")

            assertEquals("encoded", user.password)
            verify(passwordResetCodeRepository).deleteAllByEmail("test@yologram.link")
        }

        @Test
        fun `코드가 일치하지 않으면 UserPasswordResetInvalidException을 던진다`() {
            whenever(passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(resetCode()))

            assertThrows<UserPasswordResetInvalidException> {
                passwordResetService.confirm("test@yologram.link", "999999", "newpass1234")
            }

            verify(passwordEncoder, never()).encode(any())
        }

        @Test
        fun `코드가 만료되면 UserPasswordResetExpiredException을 던진다`() {
            whenever(passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(resetCode(expiredAt = LocalDateTime.now().minusMinutes(1))))

            assertThrows<UserPasswordResetExpiredException> {
                passwordResetService.confirm("test@yologram.link", "123456", "newpass1234")
            }
        }
    }
}
