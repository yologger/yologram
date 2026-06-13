package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.EmailVerificationCode
import link.yologram.api.v1.domain.ums.exception.EmailVerificationExpiredException
import link.yologram.api.v1.domain.ums.exception.EmailVerificationInvalidException
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.repository.EmailVerificationCodeRepository
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
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class EmailVerificationServiceTest {

    @Mock
    lateinit var emailVerificationCodeRepository: EmailVerificationCodeRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var emailSender: EmailSender

    @InjectMocks
    lateinit var emailVerificationService: EmailVerificationService

    @Nested
    inner class 인증_코드_발송 {

        @Test
        fun `이메일로 인증 코드를 발송한다`() {
            whenever(userRepository.existsByEmail("test@yologram.link")).thenReturn(false)
            whenever(emailVerificationCodeRepository.save(any<EmailVerificationCode>())).thenAnswer { it.arguments[0] }

            emailVerificationService.sendCode("test@yologram.link")

            verify(emailVerificationCodeRepository).deleteAllByEmail("test@yologram.link")
            verify(emailVerificationCodeRepository).save(argThat<EmailVerificationCode> {
                email == "test@yologram.link" && code.length == 6
            })
            verify(emailSender).sendVerificationCode(eq("test@yologram.link"), any())
        }

        @Test
        fun `이미 가입된 이메일이면 UserDuplicateException을 던진다`() {
            whenever(userRepository.existsByEmail("duplicate@yologram.link")).thenReturn(true)

            assertThrows<UserDuplicateException> {
                emailVerificationService.sendCode("duplicate@yologram.link")
            }

            verify(emailSender, never()).sendVerificationCode(any(), any())
        }

        @Test
        fun `기존 인증 레코드를 삭제하고 새로 생성한다`() {
            whenever(userRepository.existsByEmail("test@yologram.link")).thenReturn(false)
            whenever(emailVerificationCodeRepository.save(any<EmailVerificationCode>())).thenAnswer { it.arguments[0] }

            emailVerificationService.sendCode("test@yologram.link")

            val inOrder = inOrder(emailVerificationCodeRepository)
            inOrder.verify(emailVerificationCodeRepository).deleteAllByEmail("test@yologram.link")
            inOrder.verify(emailVerificationCodeRepository).save(any<EmailVerificationCode>())
        }
    }

    @Nested
    inner class 이메일_인증 {

        @Test
        fun `올바른 코드로 인증에 성공한다`() {
            val verification = EmailVerificationCode(
                id = 1L,
                email = "test@yologram.link",
                code = "123456",
                expiredAt = LocalDateTime.now().plusMinutes(5),
            )
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(verification))

            emailVerificationService.verifyCode("test@yologram.link", "123456")

            assertTrue(verification.verified)
        }

        @Test
        fun `인증 레코드가 없으면 EmailVerificationInvalidException을 던진다`() {
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc("unknown@yologram.link"))
                .thenReturn(Optional.empty())

            assertThrows<EmailVerificationInvalidException> {
                emailVerificationService.verifyCode("unknown@yologram.link", "123456")
            }
        }

        @Test
        fun `인증 코드가 만료되면 EmailVerificationExpiredException을 던진다`() {
            val verification = EmailVerificationCode(
                id = 1L,
                email = "test@yologram.link",
                code = "123456",
                expiredAt = LocalDateTime.now().minusMinutes(1),
            )
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(verification))

            assertThrows<EmailVerificationExpiredException> {
                emailVerificationService.verifyCode("test@yologram.link", "123456")
            }
        }

        @Test
        fun `인증 코드가 일치하지 않으면 EmailVerificationInvalidException을 던진다`() {
            val verification = EmailVerificationCode(
                id = 1L,
                email = "test@yologram.link",
                code = "123456",
                expiredAt = LocalDateTime.now().plusMinutes(5),
            )
            whenever(emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc("test@yologram.link"))
                .thenReturn(Optional.of(verification))

            assertThrows<EmailVerificationInvalidException> {
                emailVerificationService.verifyCode("test@yologram.link", "999999")
            }
        }
    }
}
