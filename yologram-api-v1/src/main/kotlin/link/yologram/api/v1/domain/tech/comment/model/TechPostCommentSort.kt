package link.yologram.api.v1.domain.tech.comment.model

/**
 * 댓글 정렬 기준. LATEST(최신순, id desc, 기본) / OLDEST(오래된순, id asc).
 * 쿼리 파라미터는 관대하게 해석: "oldest"만 OLDEST, 그 외(미지정·오타 포함)는 LATEST 기본.
 */
enum class TechPostCommentSort {
    LATEST,
    OLDEST,
    ;

    companion object {
        fun fromParam(value: String?): TechPostCommentSort =
            if (value.equals("oldest", ignoreCase = true)) OLDEST else LATEST
    }
}
