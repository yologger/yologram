package link.yologram.api.v1.domain.pms.tech.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 테크 게시글 조회 수 — 게시글의 비정규화 속성이라 pms 소유 (TechPostLikeCount·TechPostCommentCount 미러).
 * PK는 게시글 id 그대로 사용(@GeneratedValue 없음), FK 없이 컬럼만.
 *
 * 댓글 수·좋아요 수와 달리 api-v1은 이 테이블을 갱신하지 않는다 — 쓰기는 worker 전담이다.
 * 조회 이벤트를 Kinesis로 발행하고(PostViewEventPublisher) worker가 이력(tech_post_view)과 함께 적재하므로,
 * 여기서는 조회(leftJoin+coalesce) 전용 읽기 모델이다. 증감 리포지토리를 두지 않는 이유가 이것이다.
 * count가 0이거나 row가 없어도 조회는 coalesce(0)로 처리된다.
 */
@Entity
@Table(name = "tech_post_view_count")
class TechPostViewCount(
    @Id
    val postId: Long,

    @Column(nullable = false)
    var viewCount: Long = 0,
)
