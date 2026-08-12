package link.yologram.api.v1.domain.pms.tech.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 테크 게시글 좋아요 수 — 게시글의 비정규화 속성이라 pms 소유 (TechPostCommentCount 미러).
 * PK는 게시글 id 그대로 사용(@GeneratedValue 없음), FK 없이 컬럼만.
 * 원장(tech_post_like)이 진실이고 이 테이블은 표시용 캐시 — 불일치 시 원장 COUNT로 재계산 복구.
 * 갱신은 TechPostLikeCountRepository의 원자 쿼리(increase/decrease)로만 수행 —
 * 엔티티를 읽어 ++ 후 save하는 방식은 동시 요청 레이스가 있어 금지.
 * count가 0이어도 row는 삭제하지 않는다 (조회 leftJoin+coalesce가 0을 처리, 삭제/재생성 churn 제거).
 */
@Entity
@Table(name = "tech_post_like_count")
class TechPostLikeCount(
    @Id
    val postId: Long,

    @Column(nullable = false)
    var likeCount: Long = 0,
)
