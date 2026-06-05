package link.yologram.api.v1.global.exception

data class ErrorResponse(
    val errorMessage: String?,
    val errorCode: String,
)
