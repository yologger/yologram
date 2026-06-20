package link.yologram.api.v1.domain.cms.service

import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.cms.model.CategoryResponse
import link.yologram.api.v1.domain.cms.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    @Transactional(readOnly = true)
    fun getCategories(sectionPath: String): List<CategoryResponse> {
        val section = Section.fromPath(sectionPath)
        return categoryRepository.findBySectionAndIsActiveTrueOrderBySortOrderAsc(section)
            .map { CategoryResponse(id = it.id, name = it.name, sortOrder = it.sortOrder) }
    }
}
