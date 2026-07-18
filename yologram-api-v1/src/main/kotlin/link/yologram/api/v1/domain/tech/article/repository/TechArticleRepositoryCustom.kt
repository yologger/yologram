package link.yologram.api.v1.domain.tech.article.repository

import link.yologram.api.v1.domain.tech.article.entity.TechArticle
import link.yologram.api.v1.domain.tech.article.model.TechArticleCursor

interface TechArticleRepositoryCustom {
    /** 발행순(published_at desc, id desc) keyset 조회 — SUMMARIZED만 */
    fun findSummarizedArticles(cursor: TechArticleCursor?, limit: Int): List<TechArticle>
}
