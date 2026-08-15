package link.yologram.api.v1.domain.search.tech.publisher.message

/**
 * 게시글 인덱싱 작업 메시지 (SQS 페이로드) — worker와 문자열 계약으로 미러.
 *
 * 예: {"target":"TECH_POST","from":1,"to":20}
 *
 * 단건도 from == to로 보낸다 — 범위 인덱싱 한 경로로 처리해 코드가 갈리지 않게 한다.
 * 레거시(yologram-legacy)는 단건을 SQS 없이 동기 처리해 경로가 둘로 나뉘었고,
 * 그 결과 단건 경로에만 문서 변환 누락 버그가 남아 있었다(문서로 변환해두고 엔티티를 인덱싱).
 *
 * target으로 대상을 구분한다 — TECH_NEWS·INVEST_POST가 늘어도 큐를 새로 만들지 않는다.
 */
data class TechIndexingMessage(
    val target: String = TARGET_TECH_POST,
    val from: Long,
    val to: Long,
) {
    companion object {
        const val TARGET_TECH_POST = "TECH_POST"
    }
}
