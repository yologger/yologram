package link.yologram.worker.domain.tech.news.entity

import jakarta.persistence.*

/** 뉴스 ↔ 카테고리 N:M (tech_category.id 참조 — 무FK, 커뮤니티 매핑과 동일 모델) */
@Entity
@Table(name = "tech_news_category_mapping")
class TechNewsCategoryMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val newsId: Long,

    @Column(nullable = false)
    val categoryId: Long,
)
