package link.yologram.worker.domain.news.tech.repository

import link.yologram.worker.domain.news.tech.entity.TechNewsCategoryMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TechNewsCategoryMappingRepository : JpaRepository<TechNewsCategoryMapping, Long> {

    fun findByNewsId(newsId: Long): List<TechNewsCategoryMapping>

    /** 색인 시 N+1 회피 — 여러 뉴스의 매핑을 한 번에 조회한다 */
    fun findByNewsIdIn(newsIds: List<Long>): List<TechNewsCategoryMapping>

    /** 재요약 시 매핑 교체용 — @Modifying 벌크 delete (derived delete는 flush 순서로 uk 충돌 위험) */
    @Modifying
    @Query("delete from TechNewsCategoryMapping m where m.newsId = :newsId")
    fun deleteByNewsIdBulk(newsId: Long)
}
