package link.yologram.api.v1.domain.ums.model

data class AdminValidateTokenResponse(
    val uid: Long,
    val email: String,
    val name: String,
)
