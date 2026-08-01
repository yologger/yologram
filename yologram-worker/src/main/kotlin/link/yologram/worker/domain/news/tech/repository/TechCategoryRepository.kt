package link.yologram.worker.domain.news.tech.repository

import link.yologram.worker.domain.news.tech.entity.TechCategory
import org.springframework.data.jpa.repository.JpaRepository

interface TechCategoryRepository : JpaRepository<TechCategory, Long> {
    /** LLM 분류 어휘 — 활성 카테고리만 (비활성은 칩·작성·분류 모두에서 제외되는 정책) */
    fun findByIsActiveTrueOrderBySortOrder(): List<TechCategory>
}
