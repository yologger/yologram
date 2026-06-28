package link.yologram.api.v1.domain.pms.repository

import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.pms.entity.Post

interface PostRepositoryCustom {
    /**
     * 섹션 피드 (id desc), keyset 페이지네이션.
     * cursorId가 있으면 그 id보다 작은(=더 과거) 글부터 limit개 조회.
     */
    fun findPostsBySection(section: Section, categoryId: Long?, cursorId: Long?, limit: Int): List<Post>

    /**
     * 내 글 목록 (id desc), cursor 페이지네이션 — 실사용(무한스크롤).
     * userId 고정 + section(선택) 동적 조건 + cursorId 이후 과거 글. 피드와 동일 방식.
     */
    fun findMyPosts(userId: Long, section: Section?, cursorId: Long?, limit: Int): List<Post>

    /**
     * 내 글 목록 (id desc), offset 페이지네이션 — 학습용.
     * userId 고정 + section(선택) 동적 조건. cursor와 대비되는 offset+count 방식.
     */
    fun findMyPosts(userId: Long, section: Section?, offset: Long, limit: Int): List<Post>

    /** 내 글 전체 개수 (offset 페이지네이션의 totalCount용). 조건은 findMyPosts와 동일 */
    fun countMyPosts(userId: Long, section: Section?): Long
}
