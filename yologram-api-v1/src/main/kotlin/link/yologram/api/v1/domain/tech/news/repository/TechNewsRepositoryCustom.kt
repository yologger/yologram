package link.yologram.api.v1.domain.tech.news.repository

import link.yologram.api.v1.domain.tech.news.entity.TechNews
import link.yologram.api.v1.domain.tech.news.model.TechNewsCursor

interface TechNewsRepositoryCustom {
    /** 발행순(published_at desc, id desc) keyset 조회 — SUMMARIZED만, categoryId는 있을 때만 필터 */
    fun findSummarizedNews(categoryId: Long?, cursor: TechNewsCursor?, limit: Int): List<TechNews>
}
