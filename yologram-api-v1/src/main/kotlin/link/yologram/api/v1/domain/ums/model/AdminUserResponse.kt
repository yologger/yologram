package link.yologram.api.v1.domain.ums.model

import io.swagger.v3.oas.annotations.media.Schema
import link.yologram.api.v1.domain.ums.entity.AdminUser
import link.yologram.api.v1.domain.ums.enum.AdminUserRole
import link.yologram.api.v1.domain.ums.enum.UserStatus
import java.time.LocalDateTime

@Schema(description = "어드민 유저")
data class AdminUserResponse(
    val uid: Long,
    val email: String,
    val name: String,
    val status: UserStatus,
    @Schema(description = "역할 (OWNER는 삭제 불가, DB 직접 조작으로만 관리)")
    val role: AdminUserRole,
    val joinedDate: LocalDateTime,
) {
    companion object {
        fun from(admin: AdminUser) = AdminUserResponse(
            uid = admin.id,
            email = admin.email,
            name = admin.name,
            status = admin.status,
            role = admin.role,
            joinedDate = admin.joinedDate,
        )
    }
}
