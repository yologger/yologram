package link.yologram.api.v1.domain.comment.tech.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 응답 JSON 스키마는 분리 전(CommentResponse)과 동일해야 한다 — web 사용 중. */
@Schema(description = "테크 게시글 댓글 항목")
data class TechPostCommentResponse(
    @Schema(description = "댓글 ID", example = "1")
    val id: Long,

    @Schema(description = "대상 게시글 ID", example = "1155")
    val postId: Long,

    @Schema(description = "작성자")
    val author: Author,

    @Schema(description = "내용")
    val content: String,

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
}
