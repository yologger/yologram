package link.yologram.worker.domain.tech.article.entity

import jakarta.persistence.*

/** 아티클 ↔ 카테고리 N:M (tech_category.id 참조 — 무FK, 커뮤니티 매핑과 동일 모델) */
@Entity
@Table(name = "tech_article_category_mapping")
class TechArticleCategoryMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val articleId: Long,

    @Column(nullable = false)
    val categoryId: Long,
)
