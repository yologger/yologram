package link.yologram.api.v1.domain.ums.model

import link.yologram.api.v1.domain.ums.enum.AdminUserRole

data class AdminValidateTokenResponse(
    val uid: Long,
    val email: String,
    val name: String,
    val role: AdminUserRole,
)
