package link.yologram.api.v1.domain.pms.tech.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 카운트는 metrics 객체로 중첩 (상세와 동일 계약 — TechPostDetailResponse 주석 참조).
 * section 필드는 테이블 분리 후에도 호환을 위해 "TECH" 고정으로 유지.
 */
@Schema(description = "테크 게시글 목록 항목")
data class TechPostSummaryResponse(
    @Schema(description = "게시글 ID", example = "1")
    val id: Long,

    @Schema(description = "섹션 (tech 고정)", example = "TECH")
    val section: String = TechPostDetailResponse.SECTION,

    @Schema(description = "작성자")
    val author: TechPostDetailResponse.Author,

    @Schema(description = "제목", nullable = true)
    val title: String?,

    @Schema(description = "내용")
    val content: String,

    @Schema(description = "카테고리 ID 목록", example = "[1, 2]")
    val categoryIds: List<Long>,

    @Schema(description = "지표 (댓글 수·좋아요 수·likedByMe)")
    val metrics: TechPostMetrics,

    @Schema(description = "작성 시각")
    val createdAt: LocalDateTime,
)
