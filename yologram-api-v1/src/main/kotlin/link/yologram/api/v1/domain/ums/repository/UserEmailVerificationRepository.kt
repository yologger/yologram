package link.yologram.api.v1.domain.ums.repository

import link.yologram.api.v1.domain.ums.entity.UserEmailVerification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserEmailVerificationRepository : JpaRepository<UserEmailVerification, Long> {

    fun findTopByEmailOrderByCreatedAtDesc(email: String): Optional<UserEmailVerification>

    fun deleteAllByEmail(email: String)
}
