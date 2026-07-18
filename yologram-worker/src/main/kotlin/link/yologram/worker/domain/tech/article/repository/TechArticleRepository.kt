package link.yologram.worker.domain.tech.article.repository

import link.yologram.worker.domain.tech.article.entity.TechArticle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TechArticleRepository : JpaRepository<TechArticle, Long> {

    /** 이미 수집된 link만 골라 반환 — 신규 기사 필터링용 배치 조회 */
    @Query("select n.link from TechArticle n where n.link in :links")
    fun findExistingLinks(links: List<String>): List<String>
}
