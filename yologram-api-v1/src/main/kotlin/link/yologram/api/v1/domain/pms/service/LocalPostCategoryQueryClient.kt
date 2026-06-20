package link.yologram.api.v1.domain.pms.service

import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.cms.repository.PostCategoryRepository
import org.springframework.stereotype.Component

@Component
class LocalPostCategoryQueryClient(
    private val categoryRepository: PostCategoryRepository,
) : PostCategoryQueryClient {

    override fun allActiveInSection(section: Section, categoryIds: Collection<Long>): Boolean {
        val distinctIds = categoryIds.toSet()
        if (distinctIds.isEmpty()) return true
        val matched = categoryRepository.countByIdInAndSectionAndIsActiveTrue(distinctIds, section)
        return matched == distinctIds.size.toLong()
    }
}
