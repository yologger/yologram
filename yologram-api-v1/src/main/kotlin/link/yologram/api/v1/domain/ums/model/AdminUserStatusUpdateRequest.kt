package link.yologram.api.v1.domain.ums.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import link.yologram.api.v1.domain.ums.enum.UserStatus

@Schema(description = "어드민 유저 상태 변경 요청 (OWNER 전용)")
data class AdminUserStatusUpdateRequest(
    @Schema(description = "변경할 상태 (ACTIVE/INACTIVE만 허용)", example = "INACTIVE")
    val status: UserStatus,
) {
    @get:AssertTrue(message = "status는 ACTIVE 또는 INACTIVE만 허용됩니다")
    @get:Schema(hidden = true)
    val statusAllowed: Boolean
        get() = status == UserStatus.ACTIVE || status == UserStatus.INACTIVE
}
