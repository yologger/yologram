package link.yologram.api.v1.infra.client.comment

/**
 * comment 도메인 경계 호출 추상화 (게시글 삭제 시 연관 댓글 정리) — pms 등 소비 도메인이 사용.
 * 모놀리식에서는 comment 리포지토리를 직접 호출(LocalCommentApiClient)하고,
 * MSA 분리 시 이 패키지에 RestCommentApiClient + Config + dto 추가 또는 post-deleted 이벤트 발행으로 교체한다
 * (번장 bun-order-api의 infra/noti/client 구성 미러).
 */
interface CommentApiClient {
    /** postId 게시글의 댓글 전체 삭제 (고아 댓글 방지). */
    fun deleteByPostId(postId: Long)
}
