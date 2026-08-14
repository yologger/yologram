package link.yologram.worker.domain.pms.tech.service

import link.yologram.worker.domain.pms.tech.subscriber.event.PostViewEvent
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 조회 중복 판정 키(view_key) 생성 — 멱등 정책이 사는 유일한 지점.
 *
 * 형식: "{postId}:{viewer}:{viewDate}"
 * - viewer: uid가 있으면 "u{uid}", 없으면 "i{ip}", 둘 다 없으면 "unknown"
 * - viewDate: occurredAt에서 시각을 버리고 남긴 날짜 (예: 2026-08-13) — 중복 판정 단위가 하루라는 뜻
 * - 예: "1200:u12:2026-08-13", "1200:i203.0.113.7:2026-08-13"
 *
 * viewDate는 반드시 occurredAt(이벤트 발생 시각) 기준이다. 처리 시각(now)으로 만들면
 * Kinesis 재전달·워커 재기동으로 같은 레코드를 나중에 다시 처리할 때 날짜가 달라져
 * 같은 조회가 새 view_key를 얻고, uk가 충돌하지 않아 멱등이 깨진다.
 *
 * viewer가 "unknown"(uid·ip 모두 없음)이면 그 글의 익명 조회 전체가 하루 1건으로 수렴한다 —
 * 과소집계를 허용한다. 신원 근거가 없는 이벤트를 개별 조회로 세면 카운트를 임의로 부풀릴 수 있고,
 * 정상 경로에서는 producer가 최소한 remoteAddr은 채우므로 실제 발생 빈도가 낮다.
 *
 * 경계 정책: occurredAt은 producer가 KST 벽시계로 만든 값이고(전 서비스 TZ 통일 — docs/rules.md 「타임존」)
 * 오프셋 정보가 없으므로 변환하지 않고 그대로 절삭한다. 따라서 "하루" 경계는 KST 자정이다.
 * TZ 통일 전에는 producer가 UTC 벽시계를 실어 경계가 KST 09:00이었다 — 그때 발급된 키는
 * 그 기준으로 남아 있고, 이미 멱등 키로 쓰인 값이라 소급 변경하지 않는다(경계일 1건 중복은 감수).
 * 경계를 바꾸려면 이 파일의 viewDateOf만 수정한다 (다른 곳에 날짜 계산이 없다).
 */
object PostViewKeyFactory {

    private const val VIEWER_UNKNOWN = "unknown"

    fun create(event: PostViewEvent): String = create(event.postId, event.uid, event.ip, event.occurredAt)

    fun create(postId: Long, uid: Long?, ip: String?, occurredAt: LocalDateTime): String =
        "$postId:${viewerOf(uid, ip)}:${viewDateOf(occurredAt)}"

    /** 조회 날짜 — 하루 경계 정책 변경 지점 (위 주석의 KST 09:00 경계 근거 참고) */
    private fun viewDateOf(occurredAt: LocalDateTime): LocalDate = occurredAt.toLocalDate()

    private fun viewerOf(uid: Long?, ip: String?): String = when {
        uid != null -> "u$uid"
        !ip.isNullOrBlank() -> "i$ip"
        else -> VIEWER_UNKNOWN
    }
}
