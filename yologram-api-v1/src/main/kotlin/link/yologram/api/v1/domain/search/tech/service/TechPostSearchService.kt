package link.yologram.api.v1.domain.search.tech.service

import link.yologram.api.v1.domain.pms.tech.model.TechPostDetailResponse
import link.yologram.api.v1.domain.pms.tech.model.TechPostMetrics
import link.yologram.api.v1.domain.pms.tech.model.TechPostSummaryResponse
import link.yologram.api.v1.domain.pms.tech.repository.TechPostLikeRepository
import link.yologram.api.v1.domain.search.exception.BlankSearchKeywordException
import link.yologram.api.v1.domain.search.exception.SearchPageTooDeepException
import link.yologram.api.v1.domain.search.tech.document.TechPostDocument
import link.yologram.api.v1.domain.search.tech.model.TechPostSearchSort
import link.yologram.api.v1.domain.search.tech.repository.TechPostSearchRepository
import link.yologram.api.v1.global.model.ApiEnvelopPage
import link.yologram.api.v1.infra.client.ums.UmsApiClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 게시글 검색 — OpenSearch에서 문서를 찾고 목록 API와 같은 스키마로 응답한다.
 *
 * 응답을 TechPostSummaryResponse로 맞춘 이유: 프론트가 검색 결과에도 같은 카드를 쓴다.
 * 색인 문서에 없는 두 값은 여기서 채운다 —
 *   닉네임: ums 배치 조회(색인에 넣으면 닉네임 변경 때마다 재색인이 필요하다)
 *   likedByMe: 개인화 값이라 색인 대상이 아니다(선택 인증, 비로그인은 false)
 *
 * 페이징은 offset(from/size)이다. 커서로는 총건수·페이지 번호를 만들 수 없고,
 * 검색은 그 둘이 필요하다(docs/rules.md).
 */
@Service
@ConditionalOnProperty(prefix = "opensearch.main", name = ["enabled"], havingValue = "true")
class TechPostSearchService(
    private val searchRepository: TechPostSearchRepository,
    private val umsApiClient: UmsApiClient,
    private val likeRepository: TechPostLikeRepository,
) {

    fun search(
        keyword: String,
        page: Int,
        size: Int,
        sort: TechPostSearchSort,
        viewerUid: Long?,
    ): ApiEnvelopPage<TechPostSummaryResponse> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) throw BlankSearchKeywordException()

        // size·page 보정: 목록 API와 같은 상한(50). page는 음수를 0으로
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val pageNumber = page.coerceAtLeast(0)
        val from = pageNumber * pageSize

        // max_result_window 초과는 엔진이 예외를 내므로 미리 400으로 끊는다
        if (from + pageSize > MAX_RESULT_WINDOW) throw SearchPageTooDeepException()

        val result = searchRepository.search(trimmed, from = from, size = pageSize, sort = sort)

        // 닉네임·likedByMe 배치 조회 (N+1 회피 — 목록 API와 같은 방식)
        val nicknames = umsApiClient.findNicknames(result.documents.map { it.uid })
        val likedPostIds = findLikedPostIds(viewerUid, result.documents.map { it.id })

        val data = result.documents.map { doc -> toSummary(doc, nicknames[doc.uid], doc.id in likedPostIds) }

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

    private fun toSummary(doc: TechPostDocument, nickname: String?, likedByMe: Boolean) =
        TechPostSummaryResponse(
            id = doc.id,
            author = TechPostDetailResponse.Author(uid = doc.uid, nickname = nickname),
            title = doc.title,
            content = doc.content,
            categoryIds = doc.categoryIds,
            // 색인 문서는 long(OpenSearch), 응답 계약은 Int — 카운트가 Int 범위를 넘을 규모가 아니다
            metrics = TechPostMetrics(
                commentCount = doc.metrics.commentCount.toInt(),
                likeCount = doc.metrics.likeCount.toInt(),
                viewCount = doc.metrics.viewCount.toInt(),
                likedByMe = likedByMe,
            ),
            // 색인 문서에 createdAt이 없을 수는 없지만(색인 시 필수), 역직렬화 기본값 방어
            createdAt = doc.createdAt ?: LocalDateTime.MIN,
        )

    private fun findLikedPostIds(viewerUid: Long?, postIds: List<Long>): Set<Long> {
        if (viewerUid == null || postIds.isEmpty()) return emptySet()
        return likeRepository.findByUidAndPostIdIn(viewerUid, postIds).mapTo(mutableSetOf()) { it.postId }
    }

    companion object {
        /** 목록 API와 같은 상한 */
        const val MAX_PAGE_SIZE = 50

        /** OpenSearch index.max_result_window 기본값 — 인덱스 설정을 올리면 함께 올려야 한다 */
        const val MAX_RESULT_WINDOW = 10_000
    }
}
