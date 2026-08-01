package link.yologram.api.v1.domain.pms.tech.service

import link.yologram.api.v1.domain.cms.tech.repository.TechCategoryRepository
import org.springframework.stereotype.Component

@Component
class LocalTechPostCategoryQueryClient(
    private val categoryRepository: TechCategoryRepository,
) : TechPostCategoryQueryClient {

    override fun allActive(categoryIds: Collection<Long>): Boolean {
        val distinctIds = categoryIds.toSet()
        if (distinctIds.isEmpty()) return true
        val matched = categoryRepository.countByIdInAndIsActiveTrue(distinctIds)
        return matched == distinctIds.size.toLong()
    }
}
