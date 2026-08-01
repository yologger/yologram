package link.yologram.api.v1.domain.comment.tech.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import link.yologram.api.v1.domain.comment.tech.entity.QTechPostComment
import link.yologram.api.v1.domain.comment.tech.entity.TechPostComment
import link.yologram.api.v1.domain.comment.tech.model.TechPostCommentSort

class TechPostCommentRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TechPostCommentRepositoryCustom {

    override fun findByPost(postId: Long, sort: TechPostCommentSort, cursorId: Long?, limit: Int): List<TechPostComment> {
        val comment = QTechPostComment.techPostComment

        // 글(postId) 고정 + 커서 조건. 최신순은 과거(id<cursor), 오래된순은 이후(id>cursor)로 이어받는다.
        val builder = BooleanBuilder()
        builder.and(comment.postId.eq(postId))
        if (cursorId != null) {
            if (sort == TechPostCommentSort.LATEST) {
                builder.and(comment.id.lt(cursorId))
            } else {
                builder.and(comment.id.gt(cursorId))
            }
        }

        val order = if (sort == TechPostCommentSort.LATEST) comment.id.desc() else comment.id.asc()

        return queryFactory
            .selectFrom(comment)
            .where(builder)
            .orderBy(order)
            .limit(limit.toLong())
            .fetch()
    }

    // offset 페이지네이션 (학습용). cursor와 동일 조건·정렬 + offset/limit
    override fun findByPost(postId: Long, sort: TechPostCommentSort, offset: Long, limit: Int): List<TechPostComment> {
        val comment = QTechPostComment.techPostComment
        val order = if (sort == TechPostCommentSort.LATEST) comment.id.desc() else comment.id.asc()

        return queryFactory
            .selectFrom(comment)
            .where(comment.postId.eq(postId))
            .orderBy(order)
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
    }

    override fun countByPost(postId: Long): Long {
        val comment = QTechPostComment.techPostComment
        return queryFactory
            .select(comment.count())
            .from(comment)
            .where(comment.postId.eq(postId))
            .fetchOne() ?: 0L
    }
}
