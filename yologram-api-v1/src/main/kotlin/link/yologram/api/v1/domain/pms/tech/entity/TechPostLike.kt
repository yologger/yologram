package link.yologram.api.v1.domain.pms.tech.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 테크 게시글 좋아요 이력 — "누가 어떤 글에 좋아요를 눌렀나"의 진실(source of truth).
 * UNIQUE(post_id, uid)로 유저당 글당 1개 보장 — 동시 요청 uk 충돌은 no-op으로 수렴(멱등).
 * 카운트(tech_post_like_count)는 이 이력의 비정규화 — 불일치 시 이력 기준 재계산 복구.
 * 삽입은 TechPostLikeRepository.insertIgnore(네이티브)로만 — save+flush의 uk 예외는
 * Hibernate 세션을 오염시켜 같은 트랜잭션의 후속 쿼리가 깨지므로 사용하지 않는다.
 */
@Entity
@Table(
    name = "tech_post_like",
    uniqueConstraints = [UniqueConstraint(name = "uk_tech_post_like", columnNames = ["post_id", "uid"])],
)
class TechPostLike(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 대상 게시글 (FK 없이 컬럼만 — 무FK 관례, uk가 인덱스 겸용)
    @Column(nullable = false)
    val postId: Long,

    // 누른 유저 (FK 없이 — ums 도메인 경계)
    @Column(nullable = false)
    val uid: Long,

    // 삽입이 네이티브(INSERT IGNORE ... NOW(6))라 Auditing 미사용 — 쿼리에서 직접 기록
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
