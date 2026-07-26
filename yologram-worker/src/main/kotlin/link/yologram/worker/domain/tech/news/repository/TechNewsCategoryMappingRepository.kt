package link.yologram.worker.domain.tech.article.repository

import link.yologram.worker.domain.tech.article.entity.TechArticleCategoryMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TechArticleCategoryMappingRepository : JpaRepository<TechArticleCategoryMapping, Long> {

    fun findByArticleId(articleId: Long): List<TechArticleCategoryMapping>

    /** 재요약 시 매핑 교체용 — @Modifying 벌크 delete (derived delete는 flush 순서로 uk 충돌 위험) */
    @Modifying
    @Query("delete from TechArticleCategoryMapping m where m.articleId = :articleId")
    fun deleteByArticleIdBulk(articleId: Long)
}
