package link.yologram.api.v1.domain.comment.tech.service

/**
 * tech/comment → tech/post 도메인 경계 호출 추상화 (대상 게시글 존재 검증).
 * 모놀리식에서는 tech/post 리포지토리를 직접 호출(LocalTechPostQueryClient),
 * MSA 분리 시 post-api HTTP 호출 구현으로 교체한다.
 */
interface TechPostQueryClient {
    /** postId 게시글이 존재하면 true. */
    fun exists(postId: Long): Boolean
}
