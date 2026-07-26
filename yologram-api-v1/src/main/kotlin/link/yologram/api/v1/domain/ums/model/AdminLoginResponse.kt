package link.yologram.api.v1.domain.ums.model

data class AdminLoginResponse(
    val uid: Long,
    val accessToken: String,
    val email: String,
    val name: String,
)
