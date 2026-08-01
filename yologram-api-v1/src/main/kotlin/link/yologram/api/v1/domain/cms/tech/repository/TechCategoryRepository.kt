package link.yologram.api.v1.domain.cms.tech.repository

import link.yologram.api.v1.domain.cms.tech.entity.TechCategory
import org.springframework.data.jpa.repository.JpaRepository

interface TechCategoryRepository : JpaRepository<TechCategory, Long> {
    fun findByIsActiveTrueOrderBySortOrderAsc(): List<TechCategory>

    fun countByIdInAndIsActiveTrue(ids: Collection<Long>): Long
}
