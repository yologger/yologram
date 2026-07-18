package link.yologram.api.v1.domain.tech.comment.model

import link.yologram.api.v1.domain.tech.comment.exception.InvalidTechPostCommentCursorException
import java.util.Base64

/**
 * 댓글 목록 cursor (keyset 페이지네이션).
 * 정렬 기준 id(작성순=시간순)의 마지막 값을 Base64(URL-safe)로 인코딩.
 * 최신순은 id < cursor, 오래된순은 id > cursor로 이어받는다.
 */
object TechPostCommentCursor {
    fun encode(id: Long): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(id.toString().toByteArray())

    fun decode(value: String): Long =
        try {
            String(Base64.getUrlDecoder().decode(value)).toLong()
        } catch (e: Exception) {
            throw InvalidTechPostCommentCursorException()
        }
}
