package link.yologram.api.v1.infra.client.pms

/**
 * pms 도메인 경계 호출 추상화 (대상 게시글 존재 검증·댓글 수 갱신) — comment 등 소비 도메인이 사용.
 * 모놀리식에서는 pms 리포지토리를 직접 호출(LocalPmsApiClient)하고,
 * MSA 분리 시 이 패키지에 RestPmsApiClient + Config + dto를 추가해 교체한다
 * (번장 bun-order-api의 infra/noti/client 구성 미러).
 */
interface PmsApiClient {
    /** postId 게시글이 존재하면 true. */
    fun exists(postId: Long): Boolean

    /**
     * postId 게시글의 댓글 수 +1 — 댓글 도메인이 게시글 소유 카운트(tech_post_comment_count)를
     * 갱신하는 경계 지점. 댓글 작성 트랜잭션에 참여해 댓글 저장과 원자적으로 커밋/롤백된다.
     */
    fun increasePostCommentCount(postId: Long)

    /**
     * postId 게시글의 댓글 수 -1 (0 미만 방지) — 댓글 도메인이 게시글 소유 카운트를 갱신하는 경계 지점.
     * 댓글 삭제 트랜잭션에 참여해 댓글 삭제와 원자적으로 커밋/롤백된다.
     */
    fun decreasePostCommentCount(postId: Long)
}
