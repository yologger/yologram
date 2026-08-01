package link.yologram.api.v1.domain.ums.model

import io.swagger.v3.oas.annotations.media.Schema
import link.yologram.api.v1.domain.ums.entity.AdminUser
import link.yologram.api.v1.domain.ums.enum.UserStatus
import java.time.LocalDateTime

@Schema(description = "어드민 유저")
data class AdminUserResponse(
    val uid: Long,
    val email: String,
    val name: String,
    val status: UserStatus,
    val joinedDate: LocalDateTime,
) {
    companion object {
        fun from(admin: AdminUser) = AdminUserResponse(
            uid = admin.id,
            email = admin.email,
            name = admin.name,
            status = admin.status,
            joinedDate = admin.joinedDate,
        )
    }
}
