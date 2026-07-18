package link.yologram.api.v1.domain.tech.article.repository

import link.yologram.api.v1.domain.tech.article.entity.TechArticleCategoryMapping
import org.springframework.data.jpa.repository.JpaRepository

interface TechArticleCategoryMappingRepository : JpaRepository<TechArticleCategoryMapping, Long> {

    /** 목록 응답용 배치 조회 (N+1 회피 — 게시판 findByPostIds 패턴) */
    fun findByArticleIdIn(articleIds: List<Long>): List<TechArticleCategoryMapping>
}
