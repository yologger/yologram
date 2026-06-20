package link.yologram.api.v1.domain.pms.model

import link.yologram.api.v1.domain.pms.exception.InvalidCursorException
import java.util.Base64

/**
 * 게시글 피드 cursor (keyset 페이지네이션).
 * id desc 정렬 기준(id가 작성순=시간순). 마지막 글 id를 Base64(URL-safe)로 인코딩.
 */
object PostCursor {
    fun encode(id: Long): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(id.toString().toByteArray())

    fun decode(value: String): Long =
        try {
            String(Base64.getUrlDecoder().decode(value)).toLong()
        } catch (e: Exception) {
            throw InvalidCursorException()
        }
}
