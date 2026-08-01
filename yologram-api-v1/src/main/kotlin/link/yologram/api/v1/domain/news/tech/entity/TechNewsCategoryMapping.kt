package link.yologram.api.v1.domain.news.tech.entity

import jakarta.persistence.*

/** 뉴스 ↔ 카테고리 N:M (tech_category.id 참조 — worker가 LLM 분류로 채움, api-v1은 조회 전용) */
@Entity
@Table(name = "tech_news_category_mapping")
class TechNewsCategoryMapping(
    @Id
    val id: Long = 0,

    @Column(nullable = false)
    val newsId: Long,

    @Column(nullable = false)
    val categoryId: Long,
)
