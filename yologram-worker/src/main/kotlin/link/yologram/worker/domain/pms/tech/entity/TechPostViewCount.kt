package link.yologram.worker.domain.pms.tech.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 테크 게시글 조회 수 — 게시글의 비정규화 속성 (api-v1 TechPostLikeCount·TechPostCommentCount 미러).
 * PK는 게시글 id 그대로 사용(@GeneratedValue 없음), FK 없이 컬럼만.
 * 이력(tech_post_view)이 진실이고 이 테이블은 표시용 캐시 — 불일치 시 이력 COUNT로 재계산 복구.
 * 갱신은 TechPostViewCountRepository.increase(원자 upsert)로만 — 엔티티를 읽어 ++ 후 save는 레이스라 금지.
 * count가 0이어도 row는 삭제하지 않는다 (조회 leftJoin+coalesce가 0을 처리).
 *
 * 좋아요·댓글과 달리 감소(decrease)가 없다 — 조회는 취소되지 않는 단방향 누적이다.
 */
@Entity
@Table(name = "tech_post_view_count")
class TechPostViewCount(
    @Id
    val postId: Long,

    @Column(nullable = false)
    var viewCount: Long = 0,
)
