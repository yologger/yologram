package link.yologram.worker.domain.search.tech.document

import link.yologram.worker.domain.news.tech.entity.TechNews
import java.time.LocalDateTime

/**
 * 색인용 뉴스 문서 — 검색에 필요한 필드만 담는다.
 *
 * 게시글 문서와 별도 인덱스인 이유(docs/rules.md 「검색 인덱싱」):
 * 스키마가 다르고(sourceName·publishedAt vs author·metrics), _score는 인덱스별 IDF로 계산돼
 * 서로 비교하면 문서 수가 적은 쪽이 유리해진다.
 *
 * status·retryCount는 색인하지 않는다 — 요약 파이프라인의 작업 상태이지 검색 대상이 아니다.
 * 색인 대상은 SUMMARIZED 뿐이라 status가 문서에 있어도 값이 하나뿐이다.
 */
data class TechNewsDocument(
    val id: Long,
    val title: String,
    val summary: String?,
    val link: String,
    val sourceName: String,
    val categoryIds: List<Long>,
    val publishedAt: LocalDateTime,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(news: TechNews, categoryIds: List<Long>) = TechNewsDocument(
            id = news.id,
            title = news.title,
            summary = news.summary,
            link = news.link,
            sourceName = news.sourceName,
            categoryIds = categoryIds,
            publishedAt = news.publishedAt,
            createdAt = news.createdAt,
        )
    }
}
