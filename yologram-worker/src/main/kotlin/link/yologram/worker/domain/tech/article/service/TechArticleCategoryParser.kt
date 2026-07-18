package link.yologram.worker.domain.tech.article.service

import link.yologram.worker.domain.tech.article.enums.TechArticleCategory

/**
 * LLM 요약 출력에서 카테고리 섹션(**🏷️ 카테고리**)을 분리한다.
 * - summary: 카테고리 섹션을 제거한 본문 (화면 칩과 중복 표시 방지)
 * - categories: 고정 목록 매칭 1~3개. 마커 누락·파싱 실패·목록 외 값뿐이면 [기타] 폴백
 *   — 분류 실패가 요약 저장을 막지 않는다
 */
object TechArticleCategoryParser {

    private const val MARKER = "🏷️"
    private const val MAX_CATEGORIES = 3

    data class Parsed(
        val summary: String,
        val categories: List<TechArticleCategory>,
    )

    fun parse(llmOutput: String): Parsed {
        val markerIndex = llmOutput.lastIndexOf(MARKER)
        if (markerIndex < 0) {
            return Parsed(summary = llmOutput.trim(), categories = listOf(TechArticleCategory.ETC))
        }

        // 마커 앞부분이 summary — 마커를 감싸던 "**" 잔여를 정리
        val summary = llmOutput.substring(0, markerIndex).trimEnd().trimEnd('*').trimEnd()

        // 마커 이후: 헤더 줄("🏷️ 카테고리**")을 지우고 쉼표 구분 라벨 파싱
        // "AI/ML"의 '/'는 구분자가 아니므로 쉼표·개행으로만 분리
        val categories = llmOutput.substring(markerIndex)
            .removePrefix(MARKER)
            .replace("카테고리", "")
            .replace("*", "")
            .replace(":", "")
            .split(',', '\n')
            .mapNotNull { TechArticleCategory.fromLabel(it) }
            .distinct()
            .take(MAX_CATEGORIES)
            .ifEmpty { listOf(TechArticleCategory.ETC) }

        return Parsed(summary = summary, categories = categories)
    }
}
