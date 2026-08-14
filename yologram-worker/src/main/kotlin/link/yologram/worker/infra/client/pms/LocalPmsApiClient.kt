package link.yologram.worker.infra.client.pms

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * 타 도메인(pms) 테이블 접근은 이 층에서만 허용 — 도메인 간 참조 격리 지점 (LocalCmsApiClient와 같은 규칙).
 *
 * worker에 tech_post JPA 엔티티를 두지 않고 네이티브 쿼리로 읽는다:
 *   ① 인덱싱은 읽기 전용이라 영속성 컨텍스트·변경 감지가 필요 없다
 *   ② 엔티티를 복제하면 api-v1과 두 벌이 되어 스키마 변경 시 동기화 부담이 생긴다
 *   ③ 카운트 3종 조인이 한 문장으로 끝난다
 *
 * 카테고리는 1:N이라 같이 조인하면 게시글 row가 카테고리 수만큼 불어난다 —
 * 별도 조회 후 postId로 묶는다(api-v1이 목록 조회에서 쓰는 방식과 동일한 이유).
 */
@Component
class LocalPmsApiClient(
    private val entityManager: EntityManager,
) : PmsApiClient {

    @Transactional(readOnly = true)
    override fun findPostsForIndex(from: Long, to: Long): List<TechPostForIndex> {
        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createNativeQuery(POSTS_SQL)
            .setParameter("from", from)
            .setParameter("to", to)
            .resultList as List<Array<Any?>>

        if (rows.isEmpty()) return emptyList()

        val ids = rows.map { (it[0] as Number).toLong() }
        val categoriesByPostId = findCategoryIds(ids)

        return rows.map { row ->
            val id = (row[0] as Number).toLong()
            TechPostForIndex(
                id = id,
                uid = (row[1] as Number).toLong(),
                title = row[2] as String?,
                content = row[3] as String,
                categoryIds = categoriesByPostId[id].orEmpty(),
                commentCount = (row[4] as Number).toLong(),
                likeCount = (row[5] as Number).toLong(),
                viewCount = (row[6] as Number).toLong(),
                createdAt = (row[7] as Timestamp).toLocalDateTime(),
                modifiedAt = (row[8] as Timestamp?)?.toLocalDateTime(),
            )
        }
    }

    private fun findCategoryIds(postIds: List<Long>): Map<Long, List<Long>> {
        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createNativeQuery(CATEGORIES_SQL)
            .setParameter("postIds", postIds)
            .resultList as List<Array<Any?>>

        return rows.groupBy({ (it[0] as Number).toLong() }, { (it[1] as Number).toLong() })
    }

    companion object {
        /** 카운트는 1:1이라 조인해도 row가 불어나지 않는다. 없는 글은 coalesce로 0 */
        private const val POSTS_SQL = """
            SELECT p.id, p.user_id, p.title, p.content,
                   COALESCE(cc.comment_count, 0), COALESCE(lc.like_count, 0), COALESCE(vc.view_count, 0),
                   p.created_at, p.modified_date
            FROM tech_post p
            LEFT JOIN tech_post_comment_count cc ON p.id = cc.post_id
            LEFT JOIN tech_post_like_count lc ON p.id = lc.post_id
            LEFT JOIN tech_post_view_count vc ON p.id = vc.post_id
            WHERE p.id BETWEEN :from AND :to
            ORDER BY p.id
        """

        private const val CATEGORIES_SQL = """
            SELECT post_id, category_id
            FROM tech_post_category_mapping
            WHERE post_id IN (:postIds)
        """
    }
}
