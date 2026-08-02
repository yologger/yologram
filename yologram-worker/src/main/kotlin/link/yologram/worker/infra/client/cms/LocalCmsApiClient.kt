package link.yologram.worker.infra.client.cms

import org.springframework.stereotype.Component

/** 타 도메인 리포지토리(cms TechCategoryRepository) 접근은 infra/client 층에서만 허용 — 도메인 간 참조 격리 지점 */
@Component
class LocalCmsApiClient(
    private val techCategoryRepository: TechCategoryRepository,
) : CmsApiClient {

    override fun findActiveCategories(): List<TechCategory> =
        techCategoryRepository.findByIsActiveTrueOrderBySortOrder()
}
