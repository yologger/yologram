package link.yologram.api.v1.domain.ums.model

import link.yologram.api.v1.domain.ums.enum.AdminUserRole

data class AdminLoginResponse(
    val uid: Long,
    val accessToken: String,
    val email: String,
    val name: String,
    val role: AdminUserRole,
)
