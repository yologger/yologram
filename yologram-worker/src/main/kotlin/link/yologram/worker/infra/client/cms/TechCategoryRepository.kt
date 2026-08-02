package link.yologram.worker.infra.client.cms

import org.springframework.data.jpa.repository.JpaRepository

/** 타 도메인(cms) 테이블 매핑·repository는 client 층에서만 — 도메인 코드는 CmsApiClient를 경유 */
interface TechCategoryRepository : JpaRepository<TechCategory, Long> {
    /** LLM 분류 어휘 — 활성 카테고리만 (비활성은 칩·작성·분류 모두에서 제외되는 정책) */
    fun findByIsActiveTrueOrderBySortOrder(): List<TechCategory>
}
