package link.yologram.api.v1.domain.news.tech.service

import link.yologram.api.v1.domain.news.tech.model.TechNewsCursor
import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.domain.news.tech.repository.TechNewsCategoryMappingRepository
import link.yologram.api.v1.domain.news.tech.repository.TechNewsRepository
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import link.yologram.api.v1.infra.cache.TechNewsFirstPageCache
import link.yologram.api.v1.infra.client.cms.CmsApiClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TechNewsService(
    private val techNewsRepository: TechNewsRepository,
    private val techNewsCategoryMappingRepository: TechNewsCategoryMappingRepository,
    private val cmsApiClient: CmsApiClient,
    private val techNewsFirstPageCache: TechNewsFirstPageCache,
) {

    /**
     * 테크 뉴스 발행순 피드 (keyset cursor).
     * worker가 요약을 마친(SUMMARIZED) 뉴스만 노출 — COLLECTED는 몇 분 내 요약되는 일시 상태, FAILED는 제외.
     * 첫 페이지(cursor 없음)만 캐시 경유 — 커서 페이지는 키가 분산돼 히트율이 없어 기존 DB 경로 유지.
     */
    @Transactional(readOnly = true)
    fun getNewsByCursor(categoryId: Long?, cursor: String?, size: Int): ApiEnvelopCursorPage<TechNewsResponse> {
        // coerce된 값이 캐시 키에 들어가야 하므로 키 구성 전에 보정
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val decodedCursor = cursor?.let { TechNewsCursor.decode(it) }

        return if (decodedCursor == null) {
            techNewsFirstPageCache.get(categoryId, pageSize) { loadPage(categoryId, null, pageSize) }
        } else {
            loadPage(categoryId, decodedCursor, pageSize)
        }
    }

    private fun loadPage(categoryId: Long?, decodedCursor: TechNewsCursor?, pageSize: Int): ApiEnvelopCursorPage<TechNewsResponse> {
        val news = techNewsRepository.findSummarizedNews(categoryId, decodedCursor, pageSize)

        // 카테고리 배치 조회 후 tech_category 마스터에서 라벨 해석 (N+1 회피 — 게시판 패턴).
        // cms는 타 도메인이므로 리포지토리 직접 참조 대신 CmsApiClient 경유 (infra/client 경계 규칙)
        val mappings = techNewsCategoryMappingRepository.findByNewsIdIn(news.map { it.id })
        val nameById = cmsApiClient.findCategoryNames(mappings.map { it.categoryId }.distinct())
        val categoriesByNews = mappings.groupBy(
            { it.newsId },
            { nameById[it.categoryId] },
        ).mapValues { (_, names) -> names.filterNotNull() } // 삭제된 카테고리 매핑은 라벨 표시에서 제외

        val data = news.map { TechNewsResponse.from(it, categoriesByNews[it.id].orEmpty()) }

        val nextCursor = news.lastOrNull()?.let { TechNewsCursor.encode(it.publishedAt, it.id) }
        return ApiEnvelopCursorPage(data = data, nextCursor = nextCursor)
    }

    companion object {
        const val MAX_PAGE_SIZE = 50
    }
}
