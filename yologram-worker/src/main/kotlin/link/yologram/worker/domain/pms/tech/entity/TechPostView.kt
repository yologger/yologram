package link.yologram.worker.domain.pms.tech.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 테크 게시글 조회 이력 — "누가 어떤 글을 언제 봤나"의 진실(source of truth).
 * Kinesis 조회 이벤트를 소비해 worker가 적재하며, 카운트(tech_post_view_count)는 이 이력의 비정규화다.
 *
 * UNIQUE(view_key)가 멱등의 전부다 — Kinesis at-least-once 재전달과
 * "같은 사람이 새로고침해서 producer가 원본 이벤트를 여러 건 발행한 것"을 uk 하나로 동시에 흡수한다.
 * view_key 생성 규칙은 PostViewKeyFactory 한 곳에만 있다.
 *
 * 삽입은 TechPostViewRepository.insertIgnore(네이티브)로만 — save+flush의 uk 예외는
 * Hibernate 세션을 오염시켜 같은 트랜잭션의 후속 카운트 갱신이 깨지므로 사용하지 않는다
 * (api-v1 TechPostLikeRepository.insertIgnore와 동일한 판단).
 *
 * uid·ip는 집계에 쓰지 않고 사후 분석·복구용으로만 남긴다 (판정은 view_key가 전담).
 */
@Entity
@Table(
    name = "tech_post_view",
    uniqueConstraints = [UniqueConstraint(name = "uk_tech_post_view_key", columnNames = ["view_key"])],
    indexes = [
        // 글별 조회 이력 조회·카운트 재계산 복구용
        Index(name = "idx_tech_post_view_post_id", columnList = "post_id"),
        // 보관 기간 정리 스케줄러의 삭제 대상 탐색용 (occurred_at < 임계)
        Index(name = "idx_tech_post_view_occurred_at", columnList = "occurred_at"),
    ],
)
class TechPostView(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 대상 게시글 (FK 없이 컬럼만 — 무FK 관례)
    @Column(nullable = false)
    val postId: Long,

    // 조회한 유저 — 비로그인이면 null
    @Column
    val uid: Long? = null,

    // 클라이언트 IP — IPv6 완전 표기(최대 45자) 수용
    @Column(length = 45)
    val ip: String? = null,

    // 중복 판정 키 "{postId}:{viewer}:{viewDate}" — 이 컬럼의 UNIQUE가 멱등을 보장
    @Column(nullable = false, length = 120)
    val viewKey: String,

    // 이벤트 발생 시각(producer 기준) — viewDate 계산·보관 기간 판정의 기준
    @Column(nullable = false)
    val occurredAt: LocalDateTime,

    // 삽입이 네이티브(INSERT IGNORE ... NOW(6))라 Auditing 미사용 — 쿼리에서 직접 기록
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
