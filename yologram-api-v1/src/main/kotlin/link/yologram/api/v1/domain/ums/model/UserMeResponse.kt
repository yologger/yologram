package link.yologram.api.v1.domain.ums.model

import link.yologram.api.v1.domain.ums.enum.UserType
import java.time.LocalDateTime

data class UserMeResponse(
    val uid: Long,
    val email: String,
    val name: String,
    val nickname: String,
    val avatar: String?,
    val type: UserType,
    val joinedDate: LocalDateTime,
)
