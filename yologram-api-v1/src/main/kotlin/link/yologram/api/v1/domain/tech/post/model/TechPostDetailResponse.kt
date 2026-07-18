package link.yologram.api.v1.domain.tech.post.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 응답 JSON 스키마는 분리 전(PostDetailResponse)과 동일해야 한다 — web 사용 중.
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

    @Schema(description = "좋아요 수", example = "0")
    val likeCount: Int,

    @Schema(description = "댓글 수", example = "0")
    val commentCount: Int,

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
