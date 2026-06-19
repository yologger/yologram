package link.yologram.api.v1.domain.pms.entity

import jakarta.persistence.*

@Entity
@Table(name = "post_categories")
class PostCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val postId: Long,

    @Column(nullable = false)
    val categoryId: Long,
)
