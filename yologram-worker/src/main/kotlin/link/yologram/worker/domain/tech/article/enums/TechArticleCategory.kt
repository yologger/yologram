package link.yologram.worker.domain.tech.article.enums

/**
 * 테크 아티클 카테고리 — LLM이 요약 시 1~3개 분류 (커뮤니티 카테고리와 라벨 동일).
 * DB(tech_article_category_mapping.category)에는 label 문자열로 저장.
 */
enum class TechArticleCategory(val label: String) {
    FRONTEND("Frontend"),
    BACKEND("Backend"),
    AI_ML("AI/ML"),
    DEVOPS("DevOps"),
    CLOUD("Cloud"),
    SECURITY("Security"),
    ETC("기타");

    companion object {
        /** LLM 출력 라벨 → enum. 목록 외 값·오탈자는 null (호출부에서 무시) */
        fun fromLabel(raw: String): TechArticleCategory? =
            entries.firstOrNull { it.label.equals(raw.trim(), ignoreCase = true) }
    }
}
