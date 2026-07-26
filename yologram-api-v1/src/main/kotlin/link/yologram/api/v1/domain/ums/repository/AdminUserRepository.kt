package link.yologram.api.v1.domain.ums.repository

import link.yologram.api.v1.domain.ums.entity.AdminUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AdminUserRepository : JpaRepository<AdminUser, Long> {
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): Optional<AdminUser>
}
