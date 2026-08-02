package link.yologram.api.v1.infra.client.ums

/**
 * ums 도메인 경계 호출 추상화 (작성자 닉네임 조회) — pms·comment 등 소비 도메인이 공용 사용.
 * 모놀리식에서는 ums 리포지토리를 직접 호출(LocalUmsApiClient)하고,
 * MSA 분리 시 이 패키지에 RestUmsApiClient + Config + dto를 추가해 교체한다
 * (번장 bun-order-api의 infra/noti/client 구성 미러).
 */
interface UmsApiClient {
    /** uid의 닉네임. 없으면 null. */
    fun findNickname(uid: Long): String?

    /** 여러 uid의 닉네임을 한 번에 조회 (목록 N+1 방지). 없는 uid는 맵에서 제외. */
    fun findNicknames(uids: Collection<Long>): Map<Long, String>
}
