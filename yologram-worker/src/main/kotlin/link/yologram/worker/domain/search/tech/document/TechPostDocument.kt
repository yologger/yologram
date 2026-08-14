package link.yologram.worker.domain.search.tech.document

import link.yologram.worker.infra.client.pms.TechPostForIndex
import java.time.LocalDateTime

/**
 * 게시글 검색 문서 — OpenSearch에 저장되는 형태 (레거시 BoardDocument 미러).
 *
 * 닉네임을 담지 않는다: 문서에 넣으면 닉네임 변경 시 그 사용자의 모든 글을 재색인해야 한다.
 * 우리는 이미 Valkey 닉네임 캐시(ums:users:v1:nickname:{uid}, TTL 1h)가 있어
 * 검색 응답을 만들 때 api가 채우는 편이 싸다.
 *
 * metrics는 object다 — 레거시는 nested를 썼지만 1:1 관계라 nested의 비용(별도 루씬 문서·nested 쿼리)만 든다.
 */
data class TechPostDocument(
    val id: Long,
    val uid: Long,
    val title: String?,
    val content: String,
    val categoryIds: List<Long>,
    val metrics: Metrics,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime?,
) {
    data class Metrics(
        val commentCount: Long,
        val likeCount: Long,
        val viewCount: Long,
    )

    companion object {
        fun of(post: TechPostForIndex) = TechPostDocument(
            id = post.id,
            uid = post.uid,
            title = post.title,
            content = post.content,
            categoryIds = post.categoryIds,
            metrics = Metrics(
                commentCount = post.commentCount,
                likeCount = post.likeCount,
                viewCount = post.viewCount,
            ),
            createdAt = post.createdAt,
            modifiedAt = post.modifiedAt,
        )
    }
}
