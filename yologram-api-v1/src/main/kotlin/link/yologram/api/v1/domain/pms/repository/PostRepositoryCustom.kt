package link.yologram.api.v1.domain.pms.repository

import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.pms.entity.Post

interface PostRepositoryCustom {
    /**
     * 섹션 피드 (id desc), keyset 페이지네이션.
     * cursorId가 있으면 그 id보다 작은(=더 과거) 글부터 limit개 조회.
     */
    fun findPostsBySection(section: Section, categoryId: Long?, cursorId: Long?, limit: Int): List<Post>
}
