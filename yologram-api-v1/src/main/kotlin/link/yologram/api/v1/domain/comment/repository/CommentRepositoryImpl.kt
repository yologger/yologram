package link.yologram.api.v1.domain.comment.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import link.yologram.api.v1.domain.comment.entity.Comment
import link.yologram.api.v1.domain.comment.entity.QComment
import link.yologram.api.v1.domain.comment.model.CommentSort

class CommentRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CommentRepositoryCustom {

    override fun findByPost(postId: Long, sort: CommentSort, cursorId: Long?, limit: Int): List<Comment> {
        val comment = QComment.comment

        // 글(postId) 고정 + 커서 조건. 최신순은 과거(id<cursor), 오래된순은 이후(id>cursor)로 이어받는다.
        val builder = BooleanBuilder()
        builder.and(comment.postId.eq(postId))
        if (cursorId != null) {
            if (sort == CommentSort.LATEST) {
                builder.and(comment.id.lt(cursorId))
            } else {
                builder.and(comment.id.gt(cursorId))
            }
        }

        val order = if (sort == CommentSort.LATEST) comment.id.desc() else comment.id.asc()

        return queryFactory
            .selectFrom(comment)
            .where(builder)
            .orderBy(order)
            .limit(limit.toLong())
            .fetch()
    }

    // offset 페이지네이션 (학습용). cursor와 동일 조건·정렬 + offset/limit
    override fun findByPost(postId: Long, sort: CommentSort, offset: Long, limit: Int): List<Comment> {
        val comment = QComment.comment
        val order = if (sort == CommentSort.LATEST) comment.id.desc() else comment.id.asc()

        return queryFactory
            .selectFrom(comment)
            .where(comment.postId.eq(postId))
            .orderBy(order)
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
    }

    override fun countByPost(postId: Long): Long {
        val comment = QComment.comment
        return queryFactory
            .select(comment.count())
            .from(comment)
            .where(comment.postId.eq(postId))
            .fetchOne() ?: 0L
    }
}
