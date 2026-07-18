package link.yologram.api.v1.domain.tech.article.entity

import jakarta.persistence.*

/** 아티클 ↔ 카테고리 N:M (tech_category.id 참조 — worker가 LLM 분류로 채움, api-v1은 조회 전용) */
@Entity
@Table(name = "tech_article_category_mapping")
class TechArticleCategoryMapping(
    @Id
    val id: Long = 0,

    @Column(nullable = false)
    val articleId: Long,

    @Column(nullable = false)
    val categoryId: Long,
)
