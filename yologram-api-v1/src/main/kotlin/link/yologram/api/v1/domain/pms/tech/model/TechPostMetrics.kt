package link.yologram.api.v1.domain.pms.tech.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 게시글 카운트 지표 묶음 — 목록·상세 응답 공용 (레거시 product.metrics 미러, 2026-08-10 계약 확정).
 * 평면 likeCount/commentCount 필드를 대체. viewCount는 조회수 도입 시 필드 추가(무브레이킹).
 * likedByMe는 개인화 값이지만 사용자 결정으로 metrics 안에 포함 — 비로그인이면 false.
 */
@Schema(description = "게시글 지표 (카운트 묶음)")
data class TechPostMetrics(
    @Schema(description = "댓글 수", example = "2")
    val commentCount: Int,

    @Schema(description = "좋아요 수", example = "5")
    val likeCount: Int,

    @Schema(description = "로그인 유저의 좋아요 여부 (비로그인 false)", example = "false")
    val likedByMe: Boolean,
)
