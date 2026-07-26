package link.yologram.worker.domain.tech.article.entity

import jakarta.persistence.*

/**
 * 테크 카테고리 마스터 (tech_category — 게시판·아티클 공용, 어드민이 관리).
 * worker는 조회 전용: 활성 카테고리가 LLM 분류 어휘가 된다.
 */
@Entity
@Table(name = "tech_category")
class TechCategory(
    @Id
    val id: Long = 0,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false)
    val sortOrder: Int = 0,

    @Column(nullable = false)
    val isActive: Boolean = true,
)
