package link.yologram.api.v1.domain.tech.category.service

import link.yologram.api.v1.domain.tech.category.model.TechPostCategoryResponse
import link.yologram.api.v1.domain.tech.category.repository.TechPostCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TechPostCategoryService(
    private val categoryRepository: TechPostCategoryRepository,
) {

    @Transactional(readOnly = true)
    fun getCategories(): List<TechPostCategoryResponse> {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
            .map { TechPostCategoryResponse(id = it.id, name = it.name, sortOrder = it.sortOrder) }
    }
}
