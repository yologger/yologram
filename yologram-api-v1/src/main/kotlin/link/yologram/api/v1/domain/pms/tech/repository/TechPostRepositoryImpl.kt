package link.yologram.api.v1.domain.pms.tech.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import link.yologram.api.v1.domain.pms.tech.entity.QTechPost
import link.yologram.api.v1.domain.pms.tech.entity.QTechPostCategoryMapping
import link.yologram.api.v1.domain.pms.tech.entity.TechPost

class TechPostRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TechPostRepositoryCustom {

    // cursor-based pagination
    override fun findPosts(categoryId: Long?, cursorId: Long?, limit: Int): List<TechPost> {
        val post = QTechPost.techPost

        // 카테고리 동적 조건 + 커서 조건(id < cursorId, 직전 페이지보다 과거 글).
        // OFFSET 없이 인덱스 범위 스캔으로 다음 페이지를 이어받는 keyset 방식
        val builder = feedCondition(post, categoryId)
        if (cursorId != null) {
            builder.and(post.id.lt(cursorId))
        }

        // 최신순(id desc) 정렬 후 limit개. 커서+정렬이 PK(id)를 그대로 탄다
        return queryFactory
            .selectFrom(post)
            .where(builder)
            .orderBy(post.id.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findPosts(categoryId: Long?, offset: Long, limit: Int): List<TechPost> {
        val post = QTechPost.techPost
        // 테크 피드 offset 페이지네이션(학습용). cursor와 동일 조건 + offset/limit
        return queryFactory
            .selectFrom(post)
            .where(feedCondition(post, categoryId))
            .orderBy(post.id.desc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
    }

    // offset-based pagination
    override fun countPosts(categoryId: Long?): Long {
        val post = QTechPost.techPost
        // totalCount: 조건은 findPosts와 동일, count(*)만 집계
        return queryFactory
            .select(post.count())
            .from(post)
            .where(feedCondition(post, categoryId))
            .fetchOne() ?: 0L
    }

    /**
     * 테크 피드 동적 조건: categoryId는 있을 때만 EXISTS로 결합.
     * 카테고리 필터는 tech_post_category_mapping에 (post_id, categoryId) 매핑이 있는 글만 — EXISTS는
     * 매칭 1건에 단축돼 join처럼 행이 불어나지 않아 글:카테고리 1:N에서도 안전. cursor/offset/count가 공유.
     */
    private fun feedCondition(post: QTechPost, categoryId: Long?): BooleanBuilder {
        val mapping = QTechPostCategoryMapping.techPostCategoryMapping
        val builder = BooleanBuilder()
        if (categoryId != null) {
            builder.and(
                JPAExpressions
                    .selectOne()
                    .from(mapping)
                    .where(mapping.postId.eq(post.id), mapping.categoryId.eq(categoryId))
                    .exists(),
            )
        }
        return builder
    }

    override fun findMyPosts(userId: Long, cursorId: Long?, limit: Int): List<TechPost> {
        val post = QTechPost.techPost
        // 내 글 조건(userId) + cursorId 이후(과거) 글. 피드와 동일한 keyset 방식.
        // idx_tech_post_user_id(user_id, id)를 그대로 탄다
        val builder = BooleanBuilder()
        builder.and(post.userId.eq(userId))
        if (cursorId != null) {
            builder.and(post.id.lt(cursorId))
        }
        return queryFactory
            .selectFrom(post)
            .where(builder)
            .orderBy(post.id.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findMyPosts(userId: Long, offset: Long, limit: Int): List<TechPost> {
        val post = QTechPost.techPost
        // 최신순(id desc) + OFFSET/LIMIT. 피드의 keyset과 달리 페이지 번호로 건너뛰는 offset 방식
        return queryFactory
            .selectFrom(post)
            .where(post.userId.eq(userId))
            .orderBy(post.id.desc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
    }

    override fun countMyPosts(userId: Long): Long {
        val post = QTechPost.techPost
        // totalCount: 조건은 findMyPosts와 동일, count(*)만 집계
        return queryFactory
            .select(post.count())
            .from(post)
            .where(post.userId.eq(userId))
            .fetchOne() ?: 0L
    }
}
