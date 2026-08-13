package link.yologram.api.v1.domain.pms.tech.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.ConstructorExpression
import com.querydsl.core.types.Projections
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import link.yologram.api.v1.domain.pms.tech.entity.QTechPost
import link.yologram.api.v1.domain.pms.tech.entity.QTechPostCategoryMapping
import link.yologram.api.v1.domain.pms.tech.entity.QTechPostCommentCount
import link.yologram.api.v1.domain.pms.tech.entity.QTechPostLikeCount
import link.yologram.api.v1.domain.pms.tech.entity.QTechPostViewCount
import link.yologram.api.v1.domain.pms.tech.model.TechPostWithCounts

class TechPostRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TechPostRepositoryCustom {

    /**
     * 게시글 + 카운트(댓글 수·좋아요 수·조회 수) 프로젝션. 각 카운트는 1:1 카운트 테이블을 leftJoin해
     * coalesce(0)로 — count row가 없는 글(카운트 0)도 목록·상세에서 빠지지 않고 0으로 나온다.
     * 무FK라 on(post.id = count.postId)을 명시. 글:카운트가 1:1(카운트 PK=post_id)이라
     * join으로 row가 불어나지 않아 기존 정렬·커서·limit에 영향 없다 (레거시 BoardCustomRepository 패턴).
     */
    private fun withCounts(
        post: QTechPost,
        commentCount: QTechPostCommentCount,
        likeCount: QTechPostLikeCount,
        viewCount: QTechPostViewCount,
    ): ConstructorExpression<TechPostWithCounts> =
        Projections.constructor(
            TechPostWithCounts::class.java,
            post,
            commentCount.commentCount.coalesce(0L),
            likeCount.likeCount.coalesce(0L),
            viewCount.viewCount.coalesce(0L),
        )

    override fun findPostWithCounts(id: Long): TechPostWithCounts? {
        val post = QTechPost.techPost
        val commentCount = QTechPostCommentCount.techPostCommentCount
        val likeCount = QTechPostLikeCount.techPostLikeCount
        val viewCount = QTechPostViewCount.techPostViewCount

        // 상세 단건 + 카운트 (없는 글이면 null → 호출부 404)
        return queryFactory
            .select(withCounts(post, commentCount, likeCount, viewCount))
            .from(post)
            .leftJoin(commentCount).on(post.id.eq(commentCount.postId))
            .leftJoin(likeCount).on(post.id.eq(likeCount.postId))
            .leftJoin(viewCount).on(post.id.eq(viewCount.postId))
            .where(post.id.eq(id))
            .fetchOne()
    }

    // cursor-based pagination
    override fun findPosts(categoryId: Long?, cursorId: Long?, limit: Int): List<TechPostWithCounts> {
        val post = QTechPost.techPost
        val commentCount = QTechPostCommentCount.techPostCommentCount
        val likeCount = QTechPostLikeCount.techPostLikeCount
        val viewCount = QTechPostViewCount.techPostViewCount

        // 카테고리 동적 조건 + 커서 조건(id < cursorId, 직전 페이지보다 과거 글).
        // OFFSET 없이 인덱스 범위 스캔으로 다음 페이지를 이어받는 keyset 방식
        val builder = feedCondition(post, categoryId)
        if (cursorId != null) {
            builder.and(post.id.lt(cursorId))
        }

        // 최신순(id desc) 정렬 후 limit개. 커서+정렬이 PK(id)를 그대로 탄다 (1:1 join이라 커서 영향 없음)
        return queryFactory
            .select(withCounts(post, commentCount, likeCount, viewCount))
            .from(post)
            .leftJoin(commentCount).on(post.id.eq(commentCount.postId))
            .leftJoin(likeCount).on(post.id.eq(likeCount.postId))
            .leftJoin(viewCount).on(post.id.eq(viewCount.postId))
            .where(builder)
            .orderBy(post.id.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findPosts(categoryId: Long?, offset: Long, limit: Int): List<TechPostWithCounts> {
        val post = QTechPost.techPost
        val commentCount = QTechPostCommentCount.techPostCommentCount
        val likeCount = QTechPostLikeCount.techPostLikeCount
        val viewCount = QTechPostViewCount.techPostViewCount
        // 테크 피드 offset 페이지네이션(학습용). cursor와 동일 조건 + offset/limit
        return queryFactory
            .select(withCounts(post, commentCount, likeCount, viewCount))
            .from(post)
            .leftJoin(commentCount).on(post.id.eq(commentCount.postId))
            .leftJoin(likeCount).on(post.id.eq(likeCount.postId))
            .leftJoin(viewCount).on(post.id.eq(viewCount.postId))
            .where(feedCondition(post, categoryId))
            .orderBy(post.id.desc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
    }

    // offset-based pagination
    override fun countPosts(categoryId: Long?): Long {
        val post = QTechPost.techPost
        // totalCount: 조건은 findPosts와 동일, count(*)만 집계 (카운트 join 불필요)
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

    override fun findMyPosts(userId: Long, cursorId: Long?, limit: Int): List<TechPostWithCounts> {
        val post = QTechPost.techPost
        val commentCount = QTechPostCommentCount.techPostCommentCount
        val likeCount = QTechPostLikeCount.techPostLikeCount
        val viewCount = QTechPostViewCount.techPostViewCount
        // 내 글 조건(userId) + cursorId 이후(과거) 글. 피드와 동일한 keyset 방식.
        // idx_tech_post_user_id(user_id, id)를 그대로 탄다
        val builder = BooleanBuilder()
        builder.and(post.userId.eq(userId))
        if (cursorId != null) {
            builder.and(post.id.lt(cursorId))
        }
        return queryFactory
            .select(withCounts(post, commentCount, likeCount, viewCount))
            .from(post)
            .leftJoin(commentCount).on(post.id.eq(commentCount.postId))
            .leftJoin(likeCount).on(post.id.eq(likeCount.postId))
            .leftJoin(viewCount).on(post.id.eq(viewCount.postId))
            .where(builder)
            .orderBy(post.id.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findMyPosts(userId: Long, offset: Long, limit: Int): List<TechPostWithCounts> {
        val post = QTechPost.techPost
        val commentCount = QTechPostCommentCount.techPostCommentCount
        val likeCount = QTechPostLikeCount.techPostLikeCount
        val viewCount = QTechPostViewCount.techPostViewCount
        // 최신순(id desc) + OFFSET/LIMIT. 피드의 keyset과 달리 페이지 번호로 건너뛰는 offset 방식
        return queryFactory
            .select(withCounts(post, commentCount, likeCount, viewCount))
            .from(post)
            .leftJoin(commentCount).on(post.id.eq(commentCount.postId))
            .leftJoin(likeCount).on(post.id.eq(likeCount.postId))
            .leftJoin(viewCount).on(post.id.eq(viewCount.postId))
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
