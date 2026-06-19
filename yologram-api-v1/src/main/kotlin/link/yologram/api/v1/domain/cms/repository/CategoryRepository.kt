package link.yologram.api.v1.domain.cms.repository

import link.yologram.api.v1.domain.cms.entity.Category
import link.yologram.api.v1.domain.cms.enum.Section
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findBySectionAndIsActiveTrueOrderBySortOrderAsc(section: Section): List<Category>

    fun countByIdInAndSectionAndIsActiveTrue(ids: Collection<Long>, section: Section): Long
}
