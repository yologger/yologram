package link.yologram.api.v1.domain.pms.service

/**
 * pms → comment 도메인 경계 호출 추상화 (게시글 삭제 시 연관 댓글 정리).
 * 모놀리식에서는 comment 리포지토리를 직접 호출(LocalCommentCleanupClient),
 * MSA 분리 시 comment-api 호출 또는 post-deleted 이벤트 발행 구현으로 교체한다.
 */
interface CommentCleanupClient {
    /** postId 게시글의 댓글 전체 삭제 (고아 댓글 방지). */
    fun deleteByPostId(postId: Long)
}
