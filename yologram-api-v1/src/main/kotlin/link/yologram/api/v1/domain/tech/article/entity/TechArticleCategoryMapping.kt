package link.yologram.api.v1.domain.tech.article.entity

import jakarta.persistence.*

/** 아티클 ↔ 카테고리 N:M (worker가 LLM 분류로 채움 — api-v1은 조회 전용) */
@Entity
@Table(name = "tech_article_category_mapping")
class TechArticleCategoryMapping(
    @Id
    val id: Long = 0,

    @Column(nullable = false)
    val articleId: Long,

    // 카테고리 라벨 문자열 ("Frontend", "AI/ML" 등 7종 — 커뮤니티 카테고리와 동일 어휘)
    @Column(nullable = false, length = 20)
    val category: String,
)
