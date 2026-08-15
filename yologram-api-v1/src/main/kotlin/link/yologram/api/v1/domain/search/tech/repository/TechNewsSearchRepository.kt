package link.yologram.api.v1.domain.search.tech.repository

import link.yologram.api.v1.domain.search.tech.document.TechNewsDocument
import link.yologram.api.v1.domain.search.tech.model.TechSearchSort
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Operator
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Repository

/**
 * 뉴스 검색 질의 — 게시글 검색(TechPostSearchRepository)과 같은 구조이고 인덱스·필드만 다르다.
 *
 * 인덱스는 alias(tech-news-index)만 참조한다 — 실제 인덱스명(-v1)을 쓰면
 * 매핑 변경 시 v2 재색인 + alias 이동으로 무중단 전환하는 전략이 깨진다.
 *
 * 본문 대신 summary를 검색한다: 색인에 원문 본문이 없다(RSS는 요약·발췌만 주고,
 * 우리가 가진 전문은 LLM 한국어 요약이다). 사용자가 읽는 텍스트와 검색 대상이 같아진다.
 */
@Repository
class TechNewsSearchRepository(
    // ObjectProvider로 늦게 꺼낸다 — 클라이언트 빈이 @Lazy라서 여기서 직접 주입하면 즉시 생성된다.
    // 검색 설정이 없는 환경(로컬·테스트)에서도 이 리포지토리가 만들어져야 컨텍스트가 뜬다
    private val clientProvider: ObjectProvider<OpenSearchClient>,
) {

    /** 검색 결과 — 문서 목록과 전체 매칭 수(페이지 네비게이션의 분모) */
    data class Result(
        val documents: List<TechNewsDocument>,
        val totalCount: Long,
    )

    fun search(keyword: String, from: Int, size: Int, sort: TechSearchSort): Result {
        val request = SearchRequest.Builder()
            .index(INDEX_ALIAS)
            .from(from)
            .size(size)
            // 제목 가중치 2배 — 제목이 맞는 뉴스가 요약만 맞는 뉴스보다 위로 온다.
            // nori 필드는 형태소 단위로 매칭하고, standard 필드는 단어를 통째로 매칭한다.
            // 둘 다 보는 이유: nori는 사전에 없는 외래어를 문맥마다 다르게 쪼갠다
            // ("마이그레이션" → 그레|이 / 마|이|그레이|션)
            .query { q ->
                q.multiMatch { m ->
                    m.query(keyword)
                        .fields(FIELD_TITLE_BOOSTED, FIELD_TITLE_STANDARD, FIELD_SUMMARY, FIELD_SUMMARY_STANDARD)
                        // AND — 토큰 하나만 맞아도 걸리는 기본값(OR)이면 nori가 쪼갠 한 글자 토큰에
                        // 무관한 문서가 대량으로 매칭된다
                        .operator(Operator.And)
                }
            }
            .apply {
                when (sort) {
                    TechSearchSort.RELEVANCE -> {
                        sort { s -> s.score { it.order(SortOrder.Desc) } }
                        sort { s -> s.field { f -> f.field(FIELD_PUBLISHED_AT).order(SortOrder.Desc) } }
                    }
                    TechSearchSort.LATEST -> {
                        // 뉴스의 "최신"은 수집 시각이 아니라 발행 시각이다 — 목록 API 정렬과 같은 기준
                        sort { s -> s.field { f -> f.field(FIELD_PUBLISHED_AT).order(SortOrder.Desc) } }
                        sort { s -> s.score { it.order(SortOrder.Desc) } }
                    }
                }
            }
            // 총건수는 정확한 값이 필요하다 — 기본값(10000)이면 그 이상에서 "10000건"으로 고정돼
            // 페이지 네비게이션의 마지막 페이지가 틀어진다
            .trackTotalHits { t -> t.enabled(true) }
            .build()

        val response = clientProvider.getObject().search(request, TechNewsDocument::class.java)

        return Result(
            documents = response.hits().hits().mapNotNull { it.source() },
            totalCount = response.hits().total()?.value() ?: 0,
        )
    }

    companion object {
        /** alias — 실제 인덱스는 tech-news-index-v1 (worker가 생성·관리) */
        const val INDEX_ALIAS = "tech-news-index"

        private const val FIELD_TITLE_BOOSTED = "title^2"
        private const val FIELD_TITLE_STANDARD = "title.standard^2"
        private const val FIELD_SUMMARY = "summary"
        private const val FIELD_SUMMARY_STANDARD = "summary.standard"
        private const val FIELD_PUBLISHED_AT = "publishedAt"
    }
}
