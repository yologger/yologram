package link.yologram.api.v1.domain.tech.comment.repository

import link.yologram.api.v1.domain.tech.comment.entity.TechPostComment
import link.yologram.api.v1.domain.tech.comment.model.TechPostCommentSort

interface TechPostCommentRepositoryCustom {
    /**
     * 특정 글의 댓글 목록 (cursor 페이지네이션) — 실사용.
     * sort=LATEST면 id desc·id<cursorId, OLDEST면 id asc·id>cursorId로 이어받는다.
     * (post_id, id) 인덱스를 그대로 타는 keyset 방식.
     * 주의: 호출부에서 cursorId는 Long?로 타입을 명시해야 offset(Long) 오버로드로 새지 않는다.
     */
    fun findByPost(postId: Long, sort: TechPostCommentSort, cursorId: Long?, limit: Int): List<TechPostComment>

    /**
     * 특정 글의 댓글 목록 (offset 페이지네이션) — 학습용.
     * cursor 방식(cursorId 오버로드)과 대비되는 offset+count 예시. 조건(postId)·정렬은 동일.
     */
    fun findByPost(postId: Long, sort: TechPostCommentSort, offset: Long, limit: Int): List<TechPostComment>

    /** 특정 글의 댓글 전체 개수 (offset 페이지네이션의 totalCount용). */
    fun countByPost(postId: Long): Long
}
