package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.UserPasswordResetCode
import link.yologram.api.v1.domain.ums.exception.UserPasswordResetExpiredException
import link.yologram.api.v1.domain.ums.exception.UserPasswordResetInvalidException
import link.yologram.api.v1.domain.ums.exception.UserNotFoundException
import link.yologram.api.v1.domain.ums.repository.UserPasswordResetCodeRepository
import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class UserPasswordResetService(
    private val passwordResetCodeRepository: UserPasswordResetCodeRepository,
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
        val resetCode = UserPasswordResetCode(
            email = email,
            code = code,
            expiredAt = LocalDateTime.now().plusMinutes(5),
        )
        passwordResetCodeRepository.save(resetCode)

        emailSender.sendUserPasswordResetCode(email, code)
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

    private fun findValidCode(email: String, code: String): UserPasswordResetCode {
        val resetCode = passwordResetCodeRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow { UserPasswordResetInvalidException() }

        if (resetCode.expiredAt.isBefore(LocalDateTime.now())) {
            throw UserPasswordResetExpiredException()
        }

        if (resetCode.code != code) {
            throw UserPasswordResetInvalidException()
        }

        return resetCode
    }

    private fun generateCode(): String {
        return String.format("%06d", Random.nextInt(1_000_000))
    }
}
