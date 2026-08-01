package link.yologram.api.v1.domain.comment.tech.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/** 테크 게시글 댓글. 인덱스((post_id, id))는 DDL로만 관리 (엔티티 @Index 선언 금지 — 프로젝트 관례). */
@Entity
@Table(name = "tech_post_comment")
@EntityListeners(AuditingEntityListener::class)
class TechPostComment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 대상 게시글 (FK 없이 컬럼+인덱스 — tech/post 도메인 경계)
    @Column(nullable = false)
    val postId: Long,

    // 작성자 (FK 없이 — ums 도메인 경계)
    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 1000)
    var content: String,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedDate: LocalDateTime = LocalDateTime.now(),
) {
    /** 본인 댓글 수정: 내용 갱신. modifiedDate는 Auditing이 자동 갱신 */
    fun update(content: String) {
        this.content = content
    }
}
