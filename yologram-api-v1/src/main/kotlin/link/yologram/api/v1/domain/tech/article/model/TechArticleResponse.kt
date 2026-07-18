package link.yologram.api.v1.domain.tech.article.model

import io.swagger.v3.oas.annotations.media.Schema
import link.yologram.api.v1.domain.tech.article.entity.TechArticle
import java.time.LocalDateTime

@Schema(description = "테크 아티클 (RSS 수집 + LLM 요약)")
data class TechArticleResponse(
    val id: Long,
    val title: String,
    @Schema(description = "LLM 한국어 요약 (마크다운 형식)")
    val summary: String,
    @Schema(description = "원문 링크")
    val link: String,
    @Schema(description = "출처 (소스명)")
    val sourceName: String,
    val publishedAt: LocalDateTime,
) {
    companion object {
        /** SUMMARIZED만 노출하므로 summary는 항상 존재 — 방어적으로 빈 문자열 폴백 */
        fun from(article: TechArticle) = TechArticleResponse(
            id = article.id,
            title = article.title,
            summary = article.summary.orEmpty(),
            link = article.link,
            sourceName = article.sourceName,
            publishedAt = article.publishedAt,
        )
    }
}
