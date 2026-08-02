package link.yologram.api.v1.infra.client.cms

/**
 * cms 도메인 경계 호출 추상화 (카테고리 활성 검증) — pms 등 소비 도메인이 사용.
 * 모놀리식에서는 cms 리포지토리를 직접 호출(LocalCmsApiClient)하고,
 * MSA 분리 시 이 패키지에 RestCmsApiClient + Config + dto를 추가해 교체한다
 * (번장 bun-order-api의 infra/noti/client 구성 미러).
 */
interface CmsApiClient {
    /** categoryIds가 모두 테크 게시판의 활성 카테고리이면 true. 빈 목록은 true. */
    fun allActive(categoryIds: Collection<Long>): Boolean

    /** categoryId → 카테고리명 배치 조회 (라벨 해석). 존재하지 않는(삭제된) id는 결과에서 제외. */
    fun findCategoryNames(categoryIds: Collection<Long>): Map<Long, String>
}
