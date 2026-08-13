package link.yologram.worker.domain.pms.tech.subscriber.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

/**
 * 게시글 조회 이벤트 (Kinesis 소비 페이로드) — api-v1 PostViewEvent와 문자열 계약으로 미러.
 * producer는 원본만 발행하고 중복 판정(dedup)은 소비 쪽(이 워커)이 담당한다.
 *
 * 예: {"eventType":"POST_VIEW","section":"TECH","postId":1200,"uid":null,"ip":"203.0.113.7","occurredAt":"2026-08-13T00:10:00"}
 *
 * 필드 추가는 브레이킹이 아니어야 하므로 미지의 필드는 무시한다 (producer 선배포 허용).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PostViewEvent(
    /** 이벤트 종류 — POST_VIEW가 아니면 스킵 (스트림에 다른 이벤트가 섞일 때의 분기 키) */
    val eventType: String,

    /** 섹션 — 현재 TECH 전용(테이블이 섹션을 담당). 다른 섹션은 그 섹션 테이블이 생길 때 분기 */
    val section: String,

    val postId: Long,

    /** 조회한 유저 — 비로그인이면 null */
    val uid: Long? = null,

    /** 클라이언트 IP — X-Forwarded-For 첫 값, 없으면 remoteAddr (IPv6 가능) */
    val ip: String? = null,

    /** 발생 시각 — 초 단위 ISO LocalDateTime (밀리초 없음) */
    val occurredAt: LocalDateTime,
) {
    companion object {
        const val EVENT_TYPE_POST_VIEW = "POST_VIEW"
        const val SECTION_TECH = "TECH"
    }
}
