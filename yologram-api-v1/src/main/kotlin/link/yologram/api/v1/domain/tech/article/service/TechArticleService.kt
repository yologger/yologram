package link.yologram.api.v1.domain.tech.article.service

import link.yologram.api.v1.domain.tech.article.model.TechArticleCursor
import link.yologram.api.v1.domain.tech.article.model.TechArticleResponse
import link.yologram.api.v1.domain.tech.article.repository.TechArticleCategoryMappingRepository
import link.yologram.api.v1.domain.tech.article.repository.TechArticleRepository
import link.yologram.api.v1.domain.tech.category.repository.TechCategoryRepository
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TechArticleService(
    private val techArticleRepository: TechArticleRepository,
    private val techArticleCategoryMappingRepository: TechArticleCategoryMappingRepository,
    private val techCategoryRepository: TechCategoryRepository,
) {

    /**
     * 테크 아티클 발행순 피드 (keyset cursor).
     * worker가 요약을 마친(SUMMARIZED) 아티클만 노출 — COLLECTED는 몇 분 내 요약되는 일시 상태, FAILED는 제외.
     */
    @Transactional(readOnly = true)
    fun getArticlesByCursor(categoryId: Long?, cursor: String?, size: Int): ApiEnvelopCursorPage<TechArticleResponse> {
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val decodedCursor = cursor?.let { TechArticleCursor.decode(it) }

        val articles = techArticleRepository.findSummarizedArticles(categoryId, decodedCursor, pageSize)

        // 카테고리 배치 조회 후 tech_category 마스터에서 라벨 해석 (N+1 회피 — 게시판 패턴)
        val mappings = techArticleCategoryMappingRepository.findByArticleIdIn(articles.map { it.id })
        val nameById = techCategoryRepository.findAllById(mappings.map { it.categoryId }.distinct())
            .associateBy({ it.id }, { it.name })
        val categoriesByArticle = mappings.groupBy(
            { it.articleId },
            { nameById[it.categoryId] },
        ).mapValues { (_, names) -> names.filterNotNull() } // 삭제된 카테고리 매핑은 라벨 표시에서 제외

        val data = articles.map { TechArticleResponse.from(it, categoriesByArticle[it.id].orEmpty()) }

        val nextCursor = articles.lastOrNull()?.let { TechArticleCursor.encode(it.publishedAt, it.id) }
        return ApiEnvelopCursorPage(data = data, nextCursor = nextCursor)
    }

    companion object {
        const val MAX_PAGE_SIZE = 50
    }
}
