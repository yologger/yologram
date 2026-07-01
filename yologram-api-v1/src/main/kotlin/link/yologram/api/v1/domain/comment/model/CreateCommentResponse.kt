package link.yologram.api.v1.domain.comment.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "댓글 작성 응답")
data class CreateCommentResponse(
    @Schema(description = "생성된 댓글 ID", example = "1")
    val id: Long,
)
