package link.yologram.api.v1.domain.pms.tech.service

/**
 * tech/post → tech/category 도메인 경계 호출 추상화.
 * 모놀리식에서는 tech/category 리포지토리를 직접 호출(LocalTechPostCategoryQueryClient),
 * MSA 분리 시 category-api HTTP 호출 구현으로 교체한다.
 */
interface TechPostCategoryQueryClient {
    /** categoryIds가 모두 테크 게시판의 활성 카테고리이면 true. 빈 목록은 true. */
    fun allActive(categoryIds: Collection<Long>): Boolean
}
