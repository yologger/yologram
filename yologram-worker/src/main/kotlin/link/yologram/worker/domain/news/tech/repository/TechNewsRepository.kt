package link.yologram.worker.domain.news.tech.repository

import link.yologram.worker.domain.news.tech.entity.TechNews
import link.yologram.worker.domain.news.tech.enums.TechNewsStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TechNewsRepository : JpaRepository<TechNews, Long> {

    /** 이미 수집된 link만 골라 반환 — 신규 기사 필터링용 배치 조회 */
    @Query("select a.link from TechNews a where a.link in :links")
    fun findExistingLinks(links: List<String>): List<String>

    /** 요약 대상 배치 조회 — status 큐 (COLLECTED & 재시도 한도 미만) */
    /** 색인 대상 범위 조회 — 요약이 끝난 것만(COLLECTED는 summary가 없어 검색 의미가 없다) */
    fun findByIdBetweenAndStatusOrderByIdAsc(from: Long, to: Long, status: TechNewsStatus): List<TechNews>

    /** 색인 대상 id 목록 조회 — 요약 배치가 방금 SUMMARIZED로 바꾼 건들 */
    fun findByIdInAndStatus(ids: List<Long>, status: TechNewsStatus): List<TechNews>

    /** 전체 인덱싱 범위 상한 (게시글 findMaxId와 같은 근거 — count가 아니라 max id) */
    @Query("select max(n.id) from TechNews n")
    fun findMaxId(): Long?

    fun findByStatusAndRetryCountLessThan(
        status: TechNewsStatus,
        retryCount: Int,
        pageable: Pageable,
    ): List<TechNews>
}
