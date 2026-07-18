package link.yologram.api.v1.domain.tech.post.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "tech_post_category_mapping",
    uniqueConstraints = [UniqueConstraint(name = "uk_tech_post_category_mapping", columnNames = ["post_id", "category_id"])],
)
class TechPostCategoryMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val postId: Long,

    @Column(nullable = false)
    val categoryId: Long,
)
