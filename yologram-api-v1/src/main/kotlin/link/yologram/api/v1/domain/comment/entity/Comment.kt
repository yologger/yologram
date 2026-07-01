package link.yologram.api.v1.domain.comment.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "post_comment")
@EntityListeners(AuditingEntityListener::class)
class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 대상 게시글 (FK 없이 컬럼+인덱스 — pms 도메인 경계)
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
)
