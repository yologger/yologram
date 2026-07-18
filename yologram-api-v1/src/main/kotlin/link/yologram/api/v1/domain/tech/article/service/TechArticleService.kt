package link.yologram.api.v1.domain.tech.article.service

import link.yologram.api.v1.domain.tech.article.model.TechArticleCursor
import link.yologram.api.v1.domain.tech.article.model.TechArticleResponse
import link.yologram.api.v1.domain.tech.article.repository.TechArticleRepository
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TechArticleService(
    private val techArticleRepository: TechArticleRepository,
) {

    /**
     * 테크 아티클 발행순 피드 (keyset cursor).
     * worker가 요약을 마친(SUMMARIZED) 아티클만 노출 — COLLECTED는 몇 분 내 요약되는 일시 상태, FAILED는 제외.
     */
    @Transactional(readOnly = true)
    fun getArticlesByCursor(cursor: String?, size: Int): ApiEnvelopCursorPage<TechArticleResponse> {
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val decodedCursor = cursor?.let { TechArticleCursor.decode(it) }

        val articles = techArticleRepository.findSummarizedArticles(decodedCursor, pageSize)
        val data = articles.map { TechArticleResponse.from(it) }

        val nextCursor = articles.lastOrNull()?.let { TechArticleCursor.encode(it.publishedAt, it.id) }
        return ApiEnvelopCursorPage(data = data, nextCursor = nextCursor)
    }

    companion object {
        const val MAX_PAGE_SIZE = 50
    }
}
