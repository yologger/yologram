package link.yologram.worker.domain.tech.news.repository

import link.yologram.worker.domain.tech.news.entity.TechNews
import link.yologram.worker.domain.tech.news.enums.TechNewsStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TechNewsRepository : JpaRepository<TechNews, Long> {

    /** 이미 수집된 link만 골라 반환 — 신규 기사 필터링용 배치 조회 */
    @Query("select a.link from TechNews a where a.link in :links")
    fun findExistingLinks(links: List<String>): List<String>

    /** 요약 대상 배치 조회 — status 큐 (COLLECTED & 재시도 한도 미만) */
    fun findByStatusAndRetryCountLessThan(
        status: TechNewsStatus,
        retryCount: Int,
        pageable: Pageable,
    ): List<TechNews>
}
