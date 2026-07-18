package link.yologram.api.v1.domain.tech.category.repository

import link.yologram.api.v1.domain.tech.category.entity.TechPostCategory
import org.springframework.data.jpa.repository.JpaRepository

interface TechPostCategoryRepository : JpaRepository<TechPostCategory, Long> {
    fun findByIsActiveTrueOrderBySortOrderAsc(): List<TechPostCategory>

    fun countByIdInAndIsActiveTrue(ids: Collection<Long>): Long
}
