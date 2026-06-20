package link.yologram.api.v1.domain.ums.model

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserPasswordResetConfirmRequest(
    @field:NotBlank(message = "이메일을 입력해주세요")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,

    @field:NotBlank(message = "인증 코드를 입력해주세요")
    @field:Size(min = 6, max = 6, message = "인증 코드는 6자리입니다")
    val code: String,

    @field:NotBlank(message = "새 비밀번호를 입력해주세요")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    val newPassword: String,
)
