package link.yologram.worker.domain.pms.tech.repository

import link.yologram.worker.domain.pms.tech.entity.TechPostViewCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** tech_post_view_count 원자 갱신 전용 (api-v1 TechPostLikeCountRepository 미러). */
interface TechPostViewCountRepository : JpaRepository<TechPostViewCount, Long> {

    /**
     * 조회 수 +delta. row가 없으면 delta로 생성 — MySQL upsert(ON DUPLICATE KEY UPDATE)로
     * "읽고 += 후 save"의 레이스 없이 DB에서 원자 갱신.
     *
     * 좋아요·댓글의 increase가 +1인 것과 달리 +delta인 이유는 배치 소비이기 때문 —
     * 한 배치에서 같은 글의 신규 조회가 여러 건 나오면 건별 호출 대신 postId당 1회로 합산해 갱신한다.
     */
    @Modifying
    @Query(
        value = "INSERT INTO tech_post_view_count (post_id, view_count) VALUES (:postId, :delta) " +
            "ON DUPLICATE KEY UPDATE view_count = view_count + :delta",
        nativeQuery = true,
    )
    fun increase(@Param("postId") postId: Long, @Param("delta") delta: Long)
}
