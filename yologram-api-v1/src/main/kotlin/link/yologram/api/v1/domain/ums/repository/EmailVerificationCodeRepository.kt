package link.yologram.api.v1.domain.ums.repository

import link.yologram.api.v1.domain.ums.entity.EmailVerificationCode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface EmailVerificationCodeRepository : JpaRepository<EmailVerificationCode, Long> {

    fun findTopByEmailOrderByCreatedAtDesc(email: String): Optional<EmailVerificationCode>

    fun deleteAllByEmail(email: String)
}
