package link.yologram.api.v1.domain.comment.service

/**
 * comment → ums 도메인 경계 호출 추상화 (작성자 정보 조회).
 * 모놀리식에서는 ums 리포지토리를 직접 호출(CommentLocalUserQueryClient),
 * MSA 분리 시 user-api HTTP 호출 구현으로 교체한다.
 */
interface UserQueryClient {
    /** 여러 uid의 닉네임을 한 번에 조회 (목록 N+1 방지). 없는 uid는 맵에서 제외. */
    fun findNicknames(uids: Collection<Long>): Map<Long, String>
}
