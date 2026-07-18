package link.yologram.api.v1.domain.tech.article.model

import link.yologram.api.v1.domain.tech.article.exception.InvalidTechArticleCursorException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * 테크 아티클 피드 cursor (keyset 페이지네이션).
 * 정렬 기준이 published_at desc라 유일하지 않음 — (publishedAt, id) 복합 커서로
 * 동일 발행 시각의 페이지 경계 누락·중복을 방지 (id가 tie-breaker).
 * "ISO발행시각|id"를 Base64(URL-safe)로 인코딩 — 클라이언트에겐 게시글 커서와 동일한 opaque 토큰.
 */
data class TechArticleCursor(
    val publishedAt: LocalDateTime,
    val id: Long,
) {
    companion object {
        private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private const val DELIMITER = "|"

        fun encode(publishedAt: LocalDateTime, id: Long): String {
            val raw = "${publishedAt.format(FORMATTER)}$DELIMITER$id"
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
        }

        fun decode(value: String): TechArticleCursor =
            try {
                val raw = String(Base64.getUrlDecoder().decode(value))
                val (publishedAt, id) = raw.split(DELIMITER, limit = 2)
                TechArticleCursor(LocalDateTime.parse(publishedAt, FORMATTER), id.toLong())
            } catch (e: Exception) {
                throw InvalidTechArticleCursorException()
            }
    }
}
