package link.yologram.api.v1.infra.client.cms

import link.yologram.api.v1.domain.cms.tech.repository.TechCategoryRepository
import org.springframework.stereotype.Component

/** 타 도메인 리포지토리(cms TechCategoryRepository) import는 infra/client 층에서만 허용 — 도메인 간 참조 격리 지점 */
@Component
class LocalCmsApiClient(
    private val categoryRepository: TechCategoryRepository,
) : CmsApiClient {

    override fun allActive(categoryIds: Collection<Long>): Boolean {
        val distinctIds = categoryIds.toSet()
        if (distinctIds.isEmpty()) return true
        val matched = categoryRepository.countByIdInAndIsActiveTrue(distinctIds)
        return matched == distinctIds.size.toLong()
    }

    override fun findCategoryNames(categoryIds: Collection<Long>): Map<Long, String> {
        if (categoryIds.isEmpty()) return emptyMap()
        return categoryRepository.findAllById(categoryIds.toSet()).associate { it.id to it.name }
    }
}
