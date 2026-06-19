package link.yologram.api.v1.domain.pms.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "게시글 작성 응답")
data class CreatePostResponse(
    @Schema(description = "생성된 게시글 ID", example = "123")
    val id: Long,
)
