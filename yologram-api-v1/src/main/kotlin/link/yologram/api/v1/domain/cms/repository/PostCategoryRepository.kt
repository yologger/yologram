package link.yologram.api.v1.domain.cms.repository

import link.yologram.api.v1.domain.cms.entity.PostCategory
import link.yologram.api.v1.domain.cms.enums.Section
import org.springframework.data.jpa.repository.JpaRepository

interface PostCategoryRepository : JpaRepository<PostCategory, Long> {
    fun findBySectionAndIsActiveTrueOrderBySortOrderAsc(section: Section): List<PostCategory>

    fun countByIdInAndSectionAndIsActiveTrue(ids: Collection<Long>, section: Section): Long
}
