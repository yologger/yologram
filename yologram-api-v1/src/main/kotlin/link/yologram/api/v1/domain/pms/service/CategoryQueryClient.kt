package link.yologram.api.v1.domain.pms.service

import link.yologram.api.v1.domain.cms.enum.Section

/**
 * pms → cms 도메인 경계 호출 추상화.
 * 모놀리식에서는 cms 리포지토리를 직접 호출(LocalCategoryQueryClient),
 * MSA 분리 시 cms-api HTTP 호출 구현으로 교체한다.
 */
interface CategoryQueryClient {
    /** categoryIds가 모두 해당 section의 활성 카테고리이면 true. 빈 목록은 true. */
    fun allActiveInSection(section: Section, categoryIds: Collection<Long>): Boolean
}
