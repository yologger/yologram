package link.yologram.api.v1.domain.pms.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "post_category_mapping",
    uniqueConstraints = [UniqueConstraint(name = "uk_post_category", columnNames = ["post_id", "category_id"])],
)
class PostCategoryMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val postId: Long,

    @Column(nullable = false)
    val categoryId: Long,
)
