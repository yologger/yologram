package link.yologram.api.v1.domain.ums.repository

import link.yologram.api.v1.domain.ums.entity.PasswordResetCode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PasswordResetCodeRepository : JpaRepository<PasswordResetCode, Long> {

    fun findTopByEmailOrderByCreatedAtDesc(email: String): Optional<PasswordResetCode>

    fun deleteAllByEmail(email: String)
}
