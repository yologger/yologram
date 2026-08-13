package link.yologram.api.v1.domain.pms.tech.publisher.event

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 게시글 조회 이벤트 (Kinesis 발행 페이로드, 필드 6개 고정).
 * 상세 조회가 성공한 뒤에만 발행하고, 중복 판정(dedup)은 소비 쪽(worker)이 담당 — producer는 원본만 보낸다.
 *
 * 예: {"eventType":"POST_VIEW","section":"TECH","postId":1200,"uid":12,"ip":"1.2.3.4","occurredAt":"2026-08-12T21:30:00"}
 */
data class PostViewEvent(
    /** 이벤트 종류 — 소비 쪽 분기 키 */
    val eventType: String = EVENT_TYPE_POST_VIEW,

    /** 섹션 — 테이블·경로가 섹션을 담당하므로 응답 DTO와 동일하게 "TECH" 고정 문자열 */
    val section: String = SECTION_TECH,

    val postId: Long,

    /** 조회한 유저 — 비로그인이면 null (선택 인증 재사용) */
    val uid: Long?,

    /** 클라이언트 IP — X-Forwarded-For 첫 값, 없으면 remoteAddr */
    val ip: String?,

    /** 발생 시각 — 기존 직렬화 규약과 동일한 초 단위 ISO LocalDateTime */
    val occurredAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
) {
    companion object {
        const val EVENT_TYPE_POST_VIEW = "POST_VIEW"
        const val SECTION_TECH = "TECH"
    }
}
