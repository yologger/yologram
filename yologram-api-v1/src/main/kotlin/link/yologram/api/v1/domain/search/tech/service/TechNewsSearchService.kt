package link.yologram.api.v1.domain.search.tech.service

import link.yologram.api.v1.config.opensearch.OpenSearchProperties
import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.domain.search.exception.BlankSearchKeywordException
import link.yologram.api.v1.domain.search.exception.SearchPageTooDeepException
import link.yologram.api.v1.domain.search.exception.SearchUnavailableException
import link.yologram.api.v1.domain.search.tech.document.TechNewsDocument
import link.yologram.api.v1.domain.search.tech.model.TechSearchSort
import link.yologram.api.v1.domain.search.tech.repository.TechNewsSearchRepository
import link.yologram.api.v1.global.model.ApiEnvelopPage
import link.yologram.api.v1.infra.client.cms.CmsApiClient
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 뉴스 검색 — OpenSearch에서 문서를 찾고 목록 API와 같은 스키마로 응답한다.
 *
 * 응답을 TechNewsResponse로 맞춘 이유: 프론트가 검색 결과에도 같은 카드를 쓴다.
 * 색인에 없는 값은 카테고리 라벨뿐이라 여기서 채운다 — 색인에는 id만 있고
 * 이름은 tech_category 마스터에서 바뀔 수 있다(이름을 색인하면 변경 때마다 재색인이 필요하다).
 * 게시글 검색이 닉네임을 ums에서 채우는 것과 같은 구조다.
 *
 * 페이징은 offset(from/size)이다. 커서로는 총건수·페이지 번호를 만들 수 없고,
 * 검색은 그 둘이 필요하다(docs/rules.md).
 */
@Service
class TechNewsSearchService(
    private val openSearchProperties: OpenSearchProperties,
    private val searchRepository: TechNewsSearchRepository,
    private val cmsApiClient: CmsApiClient,
) {

    fun search(
        keyword: String,
        page: Int,
        size: Int,
        sort: TechSearchSort,
    ): ApiEnvelopPage<TechNewsResponse> {
        // 설정이 없는 환경(로컬·테스트 기본)에서는 엔진에 붙지 않고 503으로 끊는다 (게시글 검색과 동일)
        if (!openSearchProperties.enabled) throw SearchUnavailableException()

        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) throw BlankSearchKeywordException()

        // size·page 보정: 목록 API와 같은 상한(50). page는 음수를 0으로
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val pageNumber = page.coerceAtLeast(0)
        val from = pageNumber * pageSize

        // max_result_window 초과는 엔진이 예외를 내므로 미리 400으로 끊는다
        if (from + pageSize > MAX_RESULT_WINDOW) throw SearchPageTooDeepException()

        val result = searchRepository.search(trimmed, from = from, size = pageSize, sort = sort)

        // 카테고리 라벨 배치 해석 (N+1 회피 — 뉴스 목록 API와 같은 방식).
        // cms는 타 도메인이므로 리포지토리 직접 참조 대신 CmsApiClient 경유 (infra/client 경계 규칙)
        val categoryIds = result.documents.flatMap { it.categoryIds }.distinct()
        val nameById = cmsApiClient.findCategoryNames(categoryIds)

        val data = result.documents.map { doc -> toResponse(doc, nameById) }

        val totalPages = if (pageSize == 0) 0L else (result.totalCount + pageSize - 1) / pageSize
        return ApiEnvelopPage(
            data = data,
            page = pageNumber.toLong(),
            size = pageSize.toLong(),
            totalPages = totalPages,
            totalCount = result.totalCount,
            first = pageNumber == 0,
            last = pageNumber >= (totalPages - 1).coerceAtLeast(0),
        )
    }

    private fun toResponse(doc: TechNewsDocument, nameById: Map<Long, String>) =
        TechNewsResponse(
            id = doc.id,
            title = doc.title,
            summary = doc.summary,
            link = doc.link,
            sourceName = doc.sourceName,
            // 삭제된 카테고리 매핑은 라벨 표시에서 제외 (목록 API와 같은 처리)
            categories = doc.categoryIds.mapNotNull { nameById[it] },
            // 색인 문서에 publishedAt이 없을 수는 없지만(색인 시 필수), 역직렬화 기본값 방어
            publishedAt = doc.publishedAt ?: LocalDateTime.MIN,
        )

    companion object {
        /** 목록 API와 같은 상한 */
        const val MAX_PAGE_SIZE = 50

        /** OpenSearch index.max_result_window 기본값 — 인덱스 설정을 올리면 함께 올려야 한다 */
        const val MAX_RESULT_WINDOW = 10_000
    }
}
