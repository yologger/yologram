package link.yologram.api.v1.domain.tech.category.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "테크 게시판 카테고리")
data class TechPostCategoryResponse(
    @Schema(description = "카테고리 ID", example = "1")
    val id: Long,

    @Schema(description = "카테고리 이름", example = "Frontend")
    val name: String,

    @Schema(description = "정렬 순서", example = "1")
    val sortOrder: Int,
)
