package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.PasswordResetCode
import link.yologram.api.v1.domain.ums.exception.PasswordResetExpiredException
import link.yologram.api.v1.domain.ums.exception.PasswordResetInvalidException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.repository.PasswordResetCodeRepository
import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class PasswordResetService(
    private val passwordResetCodeRepository: PasswordResetCodeRepository,
    private val userRepository: UserRepository,
    private val emailSender: EmailSender,
    private val passwordEncoder: BCryptPasswordEncoder,
) {

    @Transactional
    fun sendCode(email: String) {
        if (!userRepository.existsByEmail(email)) {
            throw UserNotFoundException()
        }

        passwordResetCodeRepository.deleteAllByEmail(email)

        val code = generateCode()
        val resetCode = PasswordResetCode(
            email = email,
            code = code,
            expiredAt = LocalDateTime.now().plusMinutes(5),
        )
        passwordResetCodeRepository.save(resetCode)

        emailSender.sendPasswordResetCode(email, code)
    }

    @Transactional
    fun verifyCode(email: String, code: String) {
        val resetCode = findValidCode(email, code)
        resetCode.verified = true
    }

    @Transactional
    fun confirm(email: String, code: String, newPassword: String) {
        findValidCode(email, code)

        val user = userRepository.findByEmail(email)
            .orElseThrow { UserNotFoundException() }
        user.password = passwordEncoder.encode(newPassword)

        passwordResetCodeRepository.deleteAllByEmail(email)
    }

    private fun findValidCode(email: String, code: String): PasswordResetCode {
        val resetCode = passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow { PasswordResetInvalidException() }

        if (resetCode.expiredAt.isBefore(LocalDateTime.now())) {
            throw PasswordResetExpiredException()
        }

        if (resetCode.code != code) {
            throw PasswordResetInvalidException()
        }

        return resetCode
    }

    private fun generateCode(): String {
        return String.format("%06d", Random.nextInt(1_000_000))
    }
}
