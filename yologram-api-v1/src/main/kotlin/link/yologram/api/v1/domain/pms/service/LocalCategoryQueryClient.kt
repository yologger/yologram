package link.yologram.api.v1.domain.pms.service

import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.cms.repository.CategoryRepository
import org.springframework.stereotype.Component

@Component
class LocalCategoryQueryClient(
    private val categoryRepository: CategoryRepository,
) : CategoryQueryClient {

    override fun allActiveInSection(section: Section, categoryIds: Collection<Long>): Boolean {
        val distinctIds = categoryIds.toSet()
        if (distinctIds.isEmpty()) return true
        val matched = categoryRepository.countByIdInAndSectionAndIsActiveTrue(distinctIds, section)
        return matched == distinctIds.size.toLong()
    }
}
