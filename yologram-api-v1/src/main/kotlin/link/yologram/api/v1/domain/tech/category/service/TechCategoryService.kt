package link.yologram.api.v1.domain.tech.category.service

import link.yologram.api.v1.domain.tech.category.model.TechCategoryResponse
import link.yologram.api.v1.domain.tech.category.repository.TechCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TechCategoryService(
    private val categoryRepository: TechCategoryRepository,
) {

    @Transactional(readOnly = true)
    fun getCategories(): List<TechCategoryResponse> {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
            .map { TechCategoryResponse(id = it.id, name = it.name, sortOrder = it.sortOrder) }
    }
}
