package link.yologram.api.v1.domain.search.tech.document

import java.time.LocalDateTime

/**
 * 색인된 게시글 문서 (읽기 전용) — worker TechPostDocument와 같은 스키마다.
 * 두 프로젝트가 문자열 계약으로 맞물리므로 필드를 바꾸면 양쪽을 동시에 고쳐야 한다.
 *
 * 작성자 닉네임은 색인에 없다(uid만) — 닉네임이 바뀔 때 재색인이 필요해지므로 넣지 않았고,
 * 응답 조립 시 ums에서 배치 조회한다(TechPostSearchService).
 */
data class TechPostDocument(
    val id: Long = 0,
    val uid: Long = 0,
    val title: String? = null,
    val content: String = "",
    val categoryIds: List<Long> = emptyList(),
    val metrics: Metrics = Metrics(),
    val createdAt: LocalDateTime? = null,
    val modifiedAt: LocalDateTime? = null,
) {
    data class Metrics(
        val commentCount: Long = 0,
        val likeCount: Long = 0,
        val viewCount: Long = 0,
    )
}
