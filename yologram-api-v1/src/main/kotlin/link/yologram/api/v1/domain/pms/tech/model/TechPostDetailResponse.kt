package link.yologram.api.v1.domain.pms.tech.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 카운트는 metrics 객체로 중첩 (레거시 product.metrics 미러, 2026-08-10 계약 전환 —
 * 평면 likeCount/commentCount 제거는 브레이킹이라 web-v1/v2 metrics 참조 전환과 한 트랙 배포).
 * section 필드는 테이블 분리 후에도 호환을 위해 "TECH" 고정으로 유지.
 */
@Schema(description = "테크 게시글 상세")
data class TechPostDetailResponse(
    @Schema(description = "게시글 ID", example = "1")
    val id: Long,

    @Schema(description = "섹션 (tech 고정)", example = "TECH")
    val section: String = SECTION,

    @Schema(description = "작성자")
    val author: Author,

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
) {
    @Schema(description = "작성자 정보")
    data class Author(
        @Schema(description = "작성자 uid", example = "12")
        val uid: Long,

        @Schema(description = "작성자 닉네임 (탈퇴/삭제 시 null)", nullable = true)
        val nickname: String?,
    )

    companion object {
        const val SECTION = "TECH"
    }
}
