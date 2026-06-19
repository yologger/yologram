package link.yologram.api.v1.domain.pms.service

import link.yologram.api.v1.domain.cms.enum.Section
import link.yologram.api.v1.domain.pms.entity.Post
import link.yologram.api.v1.domain.pms.entity.PostCategory
import link.yologram.api.v1.domain.pms.exception.InvalidCategoryException
import link.yologram.api.v1.domain.pms.model.CreatePostRequest
import link.yologram.api.v1.domain.pms.model.CreatePostResponse
import link.yologram.api.v1.domain.pms.repository.PostCategoryRepository
import link.yologram.api.v1.domain.pms.repository.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postCategoryRepository: PostCategoryRepository,
    private val categoryQueryClient: CategoryQueryClient,
) {

    @Transactional
    fun create(sectionPath: String, userId: Long, request: CreatePostRequest): CreatePostResponse {
        val section = Section.fromPath(sectionPath)
        val categoryIds = request.categoryIds.toSet()

        if (!categoryQueryClient.allActiveInSection(section, categoryIds)) {
            throw InvalidCategoryException()
        }

        val post = postRepository.save(
            Post(
                section = section,
                userId = userId,
                title = request.title?.takeIf { it.isNotBlank() },
                content = request.content!!,
            )
        )

        categoryIds.forEach { categoryId ->
            postCategoryRepository.save(PostCategory(postId = post.id, categoryId = categoryId))
        }

        return CreatePostResponse(id = post.id)
    }
}
