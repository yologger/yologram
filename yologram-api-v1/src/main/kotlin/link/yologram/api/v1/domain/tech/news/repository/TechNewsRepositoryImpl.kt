package link.yologram.api.v1.domain.tech.news.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import link.yologram.api.v1.domain.tech.news.entity.QTechNews
import link.yologram.api.v1.domain.tech.news.entity.QTechNewsCategoryMapping
import link.yologram.api.v1.domain.tech.news.entity.TechNews
import link.yologram.api.v1.domain.tech.news.enums.TechNewsStatus
import link.yologram.api.v1.domain.tech.news.model.TechNewsCursor

class TechNewsRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TechNewsRepositoryCustom {

    override fun findSummarizedNews(categoryId: Long?, cursor: TechNewsCursor?, limit: Int): List<TechNews> {
        val news = QTechNews.techNews
        val mapping = QTechNewsCategoryMapping.techNewsCategoryMapping

        // 요약 완료된 뉴스만 노출 (COLLECTED는 요약 대기, FAILED는 요약 불가 — 화면 제외 결정)
        val builder = BooleanBuilder().and(news.status.eq(TechNewsStatus.SUMMARIZED))

        // 카테고리 필터: 매핑에 해당 categoryId가 있는 글만 — EXISTS라 글:카테고리 1:N에서도 행 불어남 없음
        // (게시판 카테고리 필터와 동일 패턴, idx (category_id, news_id) 커버)
        if (categoryId != null) {
            builder.and(
                JPAExpressions
                    .selectOne()
                    .from(mapping)
                    .where(mapping.newsId.eq(news.id), mapping.categoryId.eq(categoryId))
                    .exists()
            )
        }

        // (published_at, id) 복합 keyset: 발행 시각이 더 과거이거나, 같은 시각이면 id가 더 작은 글부터.
        // 동일 발행 시각 다건(AWS What's New 등)의 페이지 경계 누락·중복을 id tie-breaker로 방지
        if (cursor != null) {
            builder.and(
                news.publishedAt.lt(cursor.publishedAt)
                    .or(news.publishedAt.eq(cursor.publishedAt).and(news.id.lt(cursor.id)))
            )
        }

        // 정렬·탐색 모두 idx_tech_news_published_at_id(published_at, id)를 탄다
        return queryFactory
            .selectFrom(news)
            .where(builder)
            .orderBy(news.publishedAt.desc(), news.id.desc())
            .limit(limit.toLong())
            .fetch()
    }
}
