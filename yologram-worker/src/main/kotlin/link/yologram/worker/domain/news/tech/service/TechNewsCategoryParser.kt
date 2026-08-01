package link.yologram.worker.domain.news.tech.service

import link.yologram.worker.domain.news.tech.entity.TechCategory

/**
 * LLM 요약 출력에서 카테고리 섹션(**🏷️ 카테고리**)을 분리한다.
 * 어휘는 tech_category 마스터(활성)에서 로드해 전달받는다 — 어드민이 카테고리를 바꾸면 분류에 자동 반영.
 * - summary: 카테고리 섹션을 제거한 본문
 * - categoryIds: 어휘 매칭 1~3개. 마커 누락·파싱 실패·목록 외 값뿐이면 폴백('기타', 없으면 빈 목록)
 *   — 분류 실패가 요약 저장을 막지 않는다
 */
object TechNewsCategoryParser {

    private const val MARKER = "🏷️"
    private const val MAX_CATEGORIES = 3
    private const val FALLBACK_LABEL = "기타"

    data class Parsed(
        val summary: String,
        val categoryIds: List<Long>,
    )

    fun parse(llmOutput: String, vocabulary: List<TechCategory>): Parsed {
        val idByLabel = vocabulary.associateBy({ it.name.lowercase() }, { it.id })
        val fallback = idByLabel[FALLBACK_LABEL.lowercase()]?.let { listOf(it) }.orEmpty()

        val markerIndex = llmOutput.lastIndexOf(MARKER)
        if (markerIndex < 0) {
            return Parsed(summary = llmOutput.trim(), categoryIds = fallback)
        }

        // 마커 앞부분이 summary — 마커를 감싸던 "**" 잔여를 정리
        val summary = llmOutput.substring(0, markerIndex).trimEnd().trimEnd('*').trimEnd()

        // 마커 이후: 헤더("카테고리")·기호 제거 후 쉼표 구분 라벨 매칭 ("AI/ML"의 '/'는 구분자 아님)
        val categoryIds = llmOutput.substring(markerIndex)
            .removePrefix(MARKER)
            .replace("카테고리", "")
            .replace("*", "")
            .replace(":", "")
            .split(',', '\n')
            .mapNotNull { idByLabel[it.trim().lowercase()] }
            .distinct()
            .take(MAX_CATEGORIES)
            .ifEmpty { fallback }

        return Parsed(summary = summary, categoryIds = categoryIds)
    }
}
