package link.yologram.api.v1.domain.tech.news.repository

import link.yologram.api.v1.domain.tech.news.entity.TechNewsCategoryMapping
import org.springframework.data.jpa.repository.JpaRepository

interface TechNewsCategoryMappingRepository : JpaRepository<TechNewsCategoryMapping, Long> {

    /** 목록 응답용 배치 조회 (N+1 회피 — 게시판 findByPostIds 패턴) */
    fun findByNewsIdIn(newsIds: List<Long>): List<TechNewsCategoryMapping>
}
