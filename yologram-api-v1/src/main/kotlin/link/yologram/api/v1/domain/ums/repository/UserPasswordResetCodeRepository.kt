package link.yologram.api.v1.domain.ums.repository

import link.yologram.api.v1.domain.ums.entity.UserPasswordResetCode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserPasswordResetCodeRepository : JpaRepository<UserPasswordResetCode, Long> {

    fun findTopByEmailOrderByCreatedAtDesc(email: String): Optional<UserPasswordResetCode>

    fun deleteAllByEmail(email: String)
}
