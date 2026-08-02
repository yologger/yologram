package link.yologram.worker.infra.client.cms

/**
 * cms 도메인 경계 호출 추상화 (카테고리 어휘 조회) — 뉴스 파이프라인 등 소비 도메인이 사용.
 * 모놀리식에서는 cms 리포지토리를 직접 호출(LocalCmsApiClient)하고,
 * MSA 분리 시 이 패키지에 RestCmsApiClient + Config + dto를 추가해 교체한다
 * (api-v1 infra/client 구성 미러).
 */
interface CmsApiClient {
    /** LLM 분류 어휘 — 활성 카테고리만 sortOrder 순 (비활성은 칩·작성·분류 모두에서 제외되는 정책) */
    fun findActiveCategories(): List<TechCategory>
}
