package link.yologram.worker.domain.tech.article.entity

import jakarta.persistence.*

/** 아티클 ↔ 카테고리 N:M (글 하나가 1~3개 카테고리 — 커뮤니티 매핑과 동일 모델, FK 없이) */
@Entity
@Table(name = "tech_article_category_mapping")
class TechArticleCategoryMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val articleId: Long,

    // TechArticleCategory.label 문자열 저장 ("AI/ML" 등)
    @Column(nullable = false, length = 20)
    val category: String,
)
