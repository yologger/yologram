package link.yologram.api.v1.domain.pms.tech.model

import link.yologram.api.v1.domain.pms.tech.entity.TechPost

/**
 * 게시글 + 댓글 수 조회 프로젝션 (QueryDSL Projections.constructor 용, 응답 DTO 아님).
 * commentCount는 tech_post_comment_count leftJoin + coalesce(0) 결과 — count row가 없는 글은 0.
 */
data class TechPostWithCommentCount(
    val post: TechPost,
    val commentCount: Long,
)
