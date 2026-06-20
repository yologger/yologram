package link.yologram.api.v1.domain.cms.service

import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.cms.model.PostCategoryResponse
import link.yologram.api.v1.domain.cms.repository.PostCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostCategoryService(
    private val categoryRepository: PostCategoryRepository,
) {

    @Transactional(readOnly = true)
    fun getPostCategories(sectionPath: String): List<PostCategoryResponse> {
        val section = Section.fromPath(sectionPath)
        return categoryRepository.findBySectionAndIsActiveTrueOrderBySortOrderAsc(section)
            .map { PostCategoryResponse(id = it.id, name = it.name, sortOrder = it.sortOrder) }
    }
}
