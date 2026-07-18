package link.yologram.api.v1.domain.tech.category.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/** 테크 게시판 카테고리. 섹션은 테이블명(tech_post_category)이 담당 — section 컬럼 없음. */
@Entity
@Table(name = "tech_post_category")
@EntityListeners(AuditingEntityListener::class)
class TechPostCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false)
    val sortOrder: Int = 0,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
