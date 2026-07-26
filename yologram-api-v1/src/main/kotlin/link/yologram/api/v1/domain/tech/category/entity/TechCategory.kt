package link.yologram.api.v1.domain.tech.category.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/** 테크 카테고리 마스터 (tech_category — 게시판·뉴스 공용, 어드민 관리 대상) */
@Entity
@Table(name = "tech_category")
@EntityListeners(AuditingEntityListener::class)
class TechCategory(
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
