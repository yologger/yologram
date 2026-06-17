package link.yologram.api.v1.domain.cms.entity

import jakarta.persistence.*
import link.yologram.api.v1.domain.cms.enum.Section
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
@EntityListeners(AuditingEntityListener::class)
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val section: Section,

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
