package link.yologram.api.v1.domain.pms.tech.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "테크 게시글 작성 응답")
data class CreateTechPostResponse(
    @Schema(description = "생성된 게시글 ID", example = "123")
    val id: Long,
)
