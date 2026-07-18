package link.yologram.api.v1.domain.tech.post.repository

import link.yologram.api.v1.domain.tech.post.entity.TechPost

interface TechPostRepositoryCustom {
    /**
     * 테크 피드 (id desc), cursor(keyset) 페이지네이션 — 실사용.
     * cursorId가 있으면 그 id보다 작은(=더 과거) 글부터 limit개 조회.
     * 주의: 호출부에서 cursorId는 Long?로 타입을 명시해야 offset(Long) 오버로드로 새지 않는다.
     */
    fun findPosts(categoryId: Long?, cursorId: Long?, limit: Int): List<TechPost>

    /**
     * 테크 피드 (id desc), offset 페이지네이션 — 학습용.
     * cursor 방식(cursorId 오버로드)과 대비되는 offset+count 예시. 조건(categoryId)은 동일.
     */
    fun findPosts(categoryId: Long?, offset: Long, limit: Int): List<TechPost>

    /** 테크 피드 전체 개수 (offset 페이지네이션의 totalCount용). 조건은 findPosts와 동일 */
    fun countPosts(categoryId: Long?): Long

    /**
     * 내 글 목록 (id desc), cursor 페이지네이션 — 실사용(무한스크롤).
     * userId 고정 + cursorId 이후 과거 글. 피드와 동일 방식.
     * 주의: 호출부에서 cursorId는 Long?로 타입을 명시해야 offset(Long) 오버로드로 새지 않는다.
     */
    fun findMyPosts(userId: Long, cursorId: Long?, limit: Int): List<TechPost>

    /**
     * 내 글 목록 (id desc), offset 페이지네이션 — 학습용.
     * cursor와 대비되는 offset+count 방식.
     */
    fun findMyPosts(userId: Long, offset: Long, limit: Int): List<TechPost>

    /** 내 글 전체 개수 (offset 페이지네이션의 totalCount용). 조건은 findMyPosts와 동일 */
    fun countMyPosts(userId: Long): Long
}
