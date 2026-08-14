package link.yologram.worker.domain.search.tech.subscriber.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * 게시글 인덱싱 작업 메시지 (SQS 페이로드) — api-v1 TechPostIndexMessage와 문자열 계약으로 미러.
 *
 * 예: {"target":"TECH_POST","from":1,"to":20}
 * 단건도 from == to로 오므로 처리 경로가 하나다.
 *
 * 필드 추가는 브레이킹이 아니어야 하므로 미지의 필드는 무시한다 (발행 쪽 선배포 허용).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TechPostIndexMessage(
    val target: String,
    val from: Long,
    val to: Long,
) {
    companion object {
        const val TARGET_TECH_POST = "TECH_POST"
    }
}
