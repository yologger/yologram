package link.yologram.api.v1.domain.tech.article.model

import link.yologram.api.v1.domain.tech.article.exception.InvalidTechArticleCursorException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals

class TechArticleCursorTest {

    @Test
    fun `인코딩한 커서를 디코딩하면 원래 값이 나온다`() {
        val publishedAt = LocalDateTime.of(2026, 7, 18, 9, 30, 15)

        val encoded = TechArticleCursor.encode(publishedAt, 123L)
        val decoded = TechArticleCursor.decode(encoded)

        assertEquals(publishedAt, decoded.publishedAt)
        assertEquals(123L, decoded.id)
    }

    @Test
    fun `초가 0인 발행 시각도 왕복된다`() {
        val publishedAt = LocalDateTime.of(2026, 7, 18, 9, 0, 0)

        val decoded = TechArticleCursor.decode(TechArticleCursor.encode(publishedAt, 1L))

        assertEquals(publishedAt, decoded.publishedAt)
    }

    @Test
    fun `base64가 아닌 값이면 예외가 발생한다`() {
        assertThrows<InvalidTechArticleCursorException> { TechArticleCursor.decode("@@@잘못된값@@@") }
    }

    @Test
    fun `형식이 다른 base64면 예외가 발생한다`() {
        val bogus = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("no-delimiter".toByteArray())

        assertThrows<InvalidTechArticleCursorException> { TechArticleCursor.decode(bogus) }
    }

    @Test
    fun `id가 숫자가 아니면 예외가 발생한다`() {
        val bogus = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("2026-07-18T09:00:00|abc".toByteArray())

        assertThrows<InvalidTechArticleCursorException> { TechArticleCursor.decode(bogus) }
    }
}
