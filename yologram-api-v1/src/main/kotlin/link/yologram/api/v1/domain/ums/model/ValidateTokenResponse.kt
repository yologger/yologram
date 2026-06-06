package link.yologram.api.v1.domain.ums.model

data class ValidateTokenResponse(
    val uid: Long,
    val email: String,
    val name: String,
    val nickname: String,
)
