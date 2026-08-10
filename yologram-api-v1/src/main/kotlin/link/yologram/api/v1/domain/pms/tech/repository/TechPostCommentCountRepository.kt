package link.yologram.api.v1.domain.pms.tech.repository

import link.yologram.api.v1.domain.pms.tech.entity.TechPostCommentCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TechPostCommentCountRepository : JpaRepository<TechPostCommentCount, Long> {

    /**
     * 댓글 수 +1 (댓글 작성 시). row가 없으면 1로 생성 — MySQL upsert(ON DUPLICATE KEY UPDATE)로
     * "읽고 ++ 후 save"의 동시 요청 레이스 없이 DB에서 원자 갱신.
     */
    @Modifying
    @Query(
        value = "INSERT INTO tech_post_comment_count (post_id, comment_count) VALUES (:postId, 1) " +
            "ON DUPLICATE KEY UPDATE comment_count = comment_count + 1",
        nativeQuery = true,
    )
    fun increase(@Param("postId") postId: Long)

    /**
     * 댓글 수 -1 (댓글 삭제 시). comment_count > 0 조건으로 음수 방어 —
     * 0이거나 row가 없으면 0건 갱신으로 무해(0에서 더 내려가지 않고, row도 만들지 않는다).
     */
    @Modifying
    @Query(
        value = "UPDATE tech_post_comment_count SET comment_count = comment_count - 1 " +
            "WHERE post_id = :postId AND comment_count > 0",
        nativeQuery = true,
    )
    fun decrease(@Param("postId") postId: Long)
}
