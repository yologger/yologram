package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.EmailVerificationCode
import link.yologram.api.v1.domain.ums.exception.EmailVerificationExpiredException
import link.yologram.api.v1.domain.ums.exception.EmailVerificationInvalidException
import link.yologram.api.v1.domain.ums.exception.UserDuplicateException
import link.yologram.api.v1.domain.ums.repository.EmailVerificationCodeRepository
import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class EmailVerificationService(
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val userRepository: UserRepository,
    private val emailSender: EmailSender,
) {

    @Transactional
    fun sendCode(email: String) {
        if (userRepository.existsByEmail(email)) {
            throw UserDuplicateException()
        }

        emailVerificationCodeRepository.deleteAllByEmail(email)

        val code = generateCode()
        val verification = EmailVerificationCode(
            email = email,
            code = code,
            expiredAt = LocalDateTime.now().plusMinutes(5),
        )
        emailVerificationCodeRepository.save(verification)

        emailSender.sendVerificationCode(email, code)
    }

    @Transactional
    fun verifyCode(email: String, code: String) {
        val verification = emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow { EmailVerificationInvalidException() }

        if (verification.expiredAt.isBefore(LocalDateTime.now())) {
            throw EmailVerificationExpiredException()
        }

        if (verification.code != code) {
            throw EmailVerificationInvalidException()
        }

        verification.verified = true
    }

    private fun generateCode(): String {
        return String.format("%06d", Random.nextInt(1_000_000))
    }
}
