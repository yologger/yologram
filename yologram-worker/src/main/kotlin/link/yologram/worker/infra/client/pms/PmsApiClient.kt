package link.yologram.worker.infra.client.pms

/**
 * pms 도메인 경계 클라이언트 — 현재는 같은 DB를 읽는 Local 구현이지만,
 * MSA로 분리하면 이 인터페이스만 Rest 구현으로 갈아끼운다 (api-v1 infra/client 규칙 미러).
 */
interface PmsApiClient {

    /** id 범위의 게시글을 카운트·카테고리와 함께 조회 (인덱싱용, 없는 id는 결과에서 빠진다) */
    fun findPostsForIndex(from: Long, to: Long): List<TechPostForIndex>
}
