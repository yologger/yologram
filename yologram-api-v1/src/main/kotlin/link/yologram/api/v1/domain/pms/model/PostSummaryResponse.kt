package link.yologram.api.v1.domain.pms.model

import io.swagger.v3.oas.annotations.media.Schema
import link.yologram.api.v1.domain.cms.enums.Section
import java.time.LocalDateTime

@Schema(description = "게시글 목록 항목")
data class PostSummaryResponse(
    @Schema(description = "게시글 ID", example = "1")
    val id: Long,

    @Schema(description = "섹션", example = "TECH")
    val section: Section,

    @Schema(description = "작성자")
    val author: PostDetailResponse.Author,

    @Schema(description = "제목", nullable = true)
    val title: String?,

    @Schema(description = "내용")
    val content: String,

    @Schema(description = "카테고리 ID 목록", example = "[1, 2]")
    val categoryIds: List<Long>,

    @Schema(description = "좋아요 수", example = "0")
    val likeCount: Int,

    @Schema(description = "댓글 수", example = "0")
    val commentCount: Int,

    @Schema(description = "작성 시각")
    val createdAt: LocalDateTime,
)
