package link.yologram.api.v1.domain.pms.tech.model

import link.yologram.api.v1.domain.pms.tech.entity.TechPost

/**
 * 게시글 + 카운트(댓글 수·좋아요 수) 조회 프로젝션 (QueryDSL Projections.constructor 용, 응답 DTO 아님).
 * 각 카운트는 tech_post_comment_count / tech_post_like_count leftJoin + coalesce(0) 결과 —
 * count row가 없는 글은 0. likedByMe는 개인화 값이라 프로젝션이 아닌 service에서 이력 배치 조회.
 */
data class TechPostWithCounts(
    val post: TechPost,
    val commentCount: Long,
    val likeCount: Long,
)
