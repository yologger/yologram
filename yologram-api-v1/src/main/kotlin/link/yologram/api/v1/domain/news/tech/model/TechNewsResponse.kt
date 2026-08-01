package link.yologram.api.v1.domain.news.tech.model

import io.swagger.v3.oas.annotations.media.Schema
import link.yologram.api.v1.domain.news.tech.entity.TechNews
import java.time.LocalDateTime

@Schema(description = "테크 뉴스 (RSS 수집 + LLM 요약)")
data class TechNewsResponse(
    val id: Long,
    val title: String,
    @Schema(description = "LLM 한국어 요약 (마크다운 형식)")
    val summary: String,
    @Schema(description = "원문 링크")
    val link: String,
    @Schema(description = "출처 (소스명)")
    val sourceName: String,
    @Schema(description = "카테고리 라벨 1~3개 (LLM 분류 — tech_category 마스터 기준)")
    val categories: List<String>,
    val publishedAt: LocalDateTime,
) {
    companion object {
        /** SUMMARIZED만 노출하므로 summary는 항상 존재 — 방어적으로 빈 문자열 폴백 */
        fun from(news: TechNews, categories: List<String>) = TechNewsResponse(
            id = news.id,
            title = news.title,
            summary = news.summary.orEmpty(),
            link = news.link,
            sourceName = news.sourceName,
            categories = categories,
            publishedAt = news.publishedAt,
        )
    }
}
