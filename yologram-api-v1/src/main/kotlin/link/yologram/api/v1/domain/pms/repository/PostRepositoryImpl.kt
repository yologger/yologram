package link.yologram.api.v1.domain.pms.repository

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
}
