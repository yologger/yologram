package link.yologram.api.v1.domain.comment.tech.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "테크 게시글 댓글 작성 응답")
data class CreateTechPostCommentResponse(
    @Schema(description = "생성된 댓글 ID", example = "1")
    val id: Long,
)
