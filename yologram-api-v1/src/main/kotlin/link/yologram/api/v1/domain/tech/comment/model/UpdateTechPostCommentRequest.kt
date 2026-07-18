package link.yologram.api.v1.domain.tech.comment.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "테크 게시글 댓글 수정 요청")
data class UpdateTechPostCommentRequest(
    @field:NotBlank(message = "내용을 입력해주세요.")
    @field:Size(max = 1000, message = "댓글은 1000자 이내로 입력해주세요.")
    @Schema(description = "내용", example = "수정된 댓글입니다")
    val content: String? = null,
)
