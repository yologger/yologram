package link.yologram.api.v1.domain.pms.repository

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.pms.entity.Post
import link.yologram.api.v1.domain.pms.entity.QPost
import link.yologram.api.v1.domain.pms.entity.QPostCategoryMapping

class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PostRepositoryCustom {

    override fun findPostsBySection(section: Section, categoryId: Long?, cursorId: Long?, limit: Int): List<Post> {
        val post = QPost.post
        val postCategory = QPostCategoryMapping.postCategoryMapping

        // 기본 조건: 해당 섹션 글만 (인덱스 idx_post_section_id의 선두 컬럼)
        val query = queryFactory
            .selectFrom(post)
            .where(post.section.eq(section))

        // 카테고리 필터(선택): post_category_mapping에 (post_id, categoryId) 매핑이 있는 글만.
        // EXISTS는 매칭 1건에 단축 → join처럼 행이 불어나지 않아 글:카테고리 1:N에서도 안전
        if (categoryId != null) {
            query.where(
                JPAExpressions
                    .selectOne()
                    .from(postCategory)
                    .where(postCategory.postId.eq(post.id), postCategory.categoryId.eq(categoryId))
                    .exists(),
            )
        }

        // 커서 조건(선택): id가 곧 작성순이므로 id < cursorId면 직전 페이지보다 과거 글.
        // OFFSET 없이 인덱스 범위 스캔으로 다음 페이지를 이어받는 keyset 방식
        if (cursorId != null) {
            query.where(post.id.lt(cursorId))
        }

        // 최신순(id desc) 정렬 후 limit개. 커서+정렬이 idx_post_section_id를 그대로 탐
        return query
            .orderBy(post.id.desc())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findMyPosts(userId: Long, section: Section?, cursorId: Long?, limit: Int): List<Post> {
        val post = QPost.post
        // 내 글 동적 조건(userId + section) + cursorId 이후(과거) 글. 피드와 동일한 keyset 방식
        val builder = myPostsCondition(post, userId, section)
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

    override fun findMyPosts(userId: Long, section: Section?, offset: Long, limit: Int): List<Post> {
        val post = QPost.post
        // 최신순(id desc) + OFFSET/LIMIT. 피드의 keyset과 달리 페이지 번호로 건너뛰는 offset 방식
        return queryFactory
            .selectFrom(post)
            .where(myPostsCondition(post, userId, section))
            .orderBy(post.id.desc())
            .offset(offset)
            .limit(limit.toLong())
            .fetch()
    }

    override fun countMyPosts(userId: Long, section: Section?): Long {
        val post = QPost.post
        // totalCount: 조건은 findMyPosts와 동일, count(*)만 집계
        return queryFactory
            .select(post.count())
            .from(post)
            .where(myPostsCondition(post, userId, section))
            .fetchOne() ?: 0L
    }

    /**
     * 내 글 동적 조건: userId는 항상, section은 있을 때만 AND로 결합.
     * BooleanBuilder로 동적 조건을 조립해 findMyPosts/countMyPosts가 동일 조건을 공유한다.
     */
    private fun myPostsCondition(post: QPost, userId: Long, section: Section?): BooleanBuilder {
        val builder = BooleanBuilder()
        builder.and(post.userId.eq(userId))
        if (section != null) {
            builder.and(post.section.eq(section))
        }
        return builder
    }
}
