package link.yologram.api.v1.domain.tech.article.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import link.yologram.api.v1.domain.tech.article.entity.QTechArticle
import link.yologram.api.v1.domain.tech.article.entity.TechArticle
import link.yologram.api.v1.domain.tech.article.enums.TechArticleStatus
import link.yologram.api.v1.domain.tech.article.model.TechArticleCursor

class TechArticleRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TechArticleRepositoryCustom {

    override fun findSummarizedArticles(cursor: TechArticleCursor?, limit: Int): List<TechArticle> {
        val article = QTechArticle.techArticle

        // 요약 완료된 아티클만 노출 (COLLECTED는 요약 대기, FAILED는 요약 불가 — 화면 제외 결정)
        val builder = BooleanBuilder().and(article.status.eq(TechArticleStatus.SUMMARIZED))

        // (published_at, id) 복합 keyset: 발행 시각이 더 과거이거나, 같은 시각이면 id가 더 작은 글부터.
        // 동일 발행 시각 다건(AWS What's New 등)의 페이지 경계 누락·중복을 id tie-breaker로 방지
        if (cursor != null) {
            builder.and(
                article.publishedAt.lt(cursor.publishedAt)
                    .or(article.publishedAt.eq(cursor.publishedAt).and(article.id.lt(cursor.id)))
            )
        }

        // 정렬·탐색 모두 idx_tech_article_published_at_id(published_at, id)를 탄다
        return queryFactory
            .selectFrom(article)
            .where(builder)
            .orderBy(article.publishedAt.desc(), article.id.desc())
            .limit(limit.toLong())
            .fetch()
    }
}
