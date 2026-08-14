package link.yologram.api.v1.domain.search.tech.model

/**
 * 검색 정렬 기준 — 프론트의 sort 파라미터와 같은 값.
 *
 * 어느 쪽이든 2차 키를 둔다: 1차 키가 동점일 때 순서가 흔들리면
 * 페이징에서 같은 문서가 두 페이지에 나오거나 아예 빠진다.
 */
enum class TechPostSearchSort {
    /** 연관도순 — 점수 내림차순, 동점이면 최신순 */
    RELEVANCE,

    /** 최신순 — 작성 시각 내림차순, 동시각이면 점수순 */
    LATEST,
}
