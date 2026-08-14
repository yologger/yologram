package link.yologram.api.v1.domain.search.tech.repository

import link.yologram.api.v1.domain.search.tech.document.TechPostDocument
import link.yologram.api.v1.domain.search.tech.model.TechPostSearchSort
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Repository

/**
 * 게시글 검색 질의 — OpenSearch 접근은 이 층에만 둔다(서비스는 문서·총건수만 받는다).
 *
 * 인덱스는 alias(tech-post-index)만 참조한다 — 실제 인덱스명(-v1)을 쓰면
 * 매핑 변경 시 v2 재색인 + alias 이동으로 무중단 전환하는 전략이 깨진다.
 */
@Repository
class TechPostSearchRepository(
    // ObjectProvider로 늦게 꺼낸다 — 클라이언트 빈이 @Lazy라서 여기서 직접 주입하면 즉시 생성된다.
    // 검색 설정이 없는 환경(로컬·테스트)에서도 이 리포지토리가 만들어져야 컨텍스트가 뜬다
    private val clientProvider: ObjectProvider<OpenSearchClient>,
) {

    /** 검색 결과 — 문서 목록과 전체 매칭 수(페이지 네비게이션의 분모) */
    data class Result(
        val documents: List<TechPostDocument>,
        val totalCount: Long,
    )

    fun search(keyword: String, from: Int, size: Int, sort: TechPostSearchSort): Result {
        val request = SearchRequest.Builder()
            .index(INDEX_ALIAS)
            .from(from)
            .size(size)
            // 제목 가중치 2배 — 제목이 맞는 글이 본문만 맞는 글보다 위로 온다.
            // nori 분석기가 적용된 필드라 형태소 단위로 매칭된다("검색 기능을" → 검색·기능)
            .query { q ->
                q.multiMatch { m ->
                    m.query(keyword).fields(FIELD_TITLE_BOOSTED, FIELD_CONTENT)
                }
            }
            .apply {
                when (sort) {
                    TechPostSearchSort.RELEVANCE -> {
                        sort { s -> s.score { it.order(SortOrder.Desc) } }
                        sort { s -> s.field { f -> f.field(FIELD_CREATED_AT).order(SortOrder.Desc) } }
                    }
                    TechPostSearchSort.LATEST -> {
                        sort { s -> s.field { f -> f.field(FIELD_CREATED_AT).order(SortOrder.Desc) } }
                        sort { s -> s.score { it.order(SortOrder.Desc) } }
                    }
                }
            }
            // 총건수는 정확한 값이 필요하다 — 기본값(10000)이면 그 이상에서 "10000건"으로 고정돼
            // 페이지 네비게이션의 마지막 페이지가 틀어진다
            .trackTotalHits { t -> t.enabled(true) }
            .build()

        val response = clientProvider.getObject().search(request, TechPostDocument::class.java)

        return Result(
            documents = response.hits().hits().mapNotNull { it.source() },
            totalCount = response.hits().total()?.value() ?: 0,
        )
    }

    companion object {
        /** alias — 실제 인덱스는 tech-post-index-v1 (worker가 생성·관리) */
        const val INDEX_ALIAS = "tech-post-index"

        private const val FIELD_TITLE_BOOSTED = "title^2"
        private const val FIELD_CONTENT = "content"
        private const val FIELD_CREATED_AT = "createdAt"
    }
}
