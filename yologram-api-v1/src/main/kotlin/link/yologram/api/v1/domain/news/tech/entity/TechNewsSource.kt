package link.yologram.api.v1.domain.news.tech.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 테크 뉴스 소스 (RSS 피드) — worker의 TechNewsSource 엔티티와 동일 매핑 (prod hbm2ddl=validate).
 * 수집은 worker 소관, api-v1은 어드민 CRUD 담당.
 */
@Entity
@Table(name = "tech_news_source")
@EntityListeners(AuditingEntityListener::class)
class TechNewsSource(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String,

    // RSS 피드 URL
    @Column(nullable = false, length = 500)
    var url: String,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedDate: LocalDateTime = LocalDateTime.now(),
)
