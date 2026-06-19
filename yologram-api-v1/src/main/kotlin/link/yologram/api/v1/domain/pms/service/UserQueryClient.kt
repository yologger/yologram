package link.yologram.api.v1.domain.pms.service

/**
 * pms → ums 도메인 경계 호출 추상화 (작성자 정보 조회).
 * 모놀리식에서는 ums 리포지토리를 직접 호출(LocalUserQueryClient),
 * MSA 분리 시 user-api HTTP 호출 구현으로 교체한다.
 */
interface UserQueryClient {
    /** uid의 닉네임. 없으면 null. */
    fun findNickname(uid: Long): String?
}
