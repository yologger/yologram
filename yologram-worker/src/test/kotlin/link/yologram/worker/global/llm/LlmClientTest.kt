package link.yologram.worker.global.llm

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.model.ChatModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlmClientTest {

    private val gemini: ChatModel = mock()
    private val groq: ChatModel = mock()
    private val client = LlmClient(listOf(LlmProvider("gemini", gemini), LlmProvider("groq", groq)))

    @Test
    fun `1순위 제공자가 성공하면 다음 제공자를 호출하지 않는다`() {
        whenever(gemini.call(any<String>())).thenReturn("요약 결과")

        val completion = client.complete("프롬프트")

        assertEquals("gemini", completion.provider)
        assertEquals("요약 결과", completion.content)
        verify(groq, never()).call(any<String>())
    }

    @Test
    fun `1순위가 실패하면 2순위로 fallback한다`() {
        whenever(gemini.call(any<String>())).doThrow(RuntimeException("429 Too Many Requests"))
        whenever(groq.call(any<String>())).thenReturn("fallback 요약")

        val completion = client.complete("프롬프트")

        assertEquals("groq", completion.provider)
        assertEquals("fallback 요약", completion.content)
    }

    @Test
    fun `1순위가 빈 응답이면 2순위로 fallback한다`() {
        whenever(gemini.call(any<String>())).thenReturn("  ")
        whenever(groq.call(any<String>())).thenReturn("fallback 요약")

        val completion = client.complete("프롬프트")

        assertEquals("groq", completion.provider)
    }

    @Test
    fun `응답의 앞뒤 공백은 트림한다`() {
        whenever(gemini.call(any<String>())).thenReturn("\n요약 결과\n")

        assertEquals("요약 결과", client.complete("프롬프트").content)
    }

    @Test
    fun `모든 제공자가 실패하면 예외가 발생한다`() {
        whenever(gemini.call(any<String>())).doThrow(RuntimeException("500"))
        whenever(groq.call(any<String>())).doThrow(RuntimeException("timeout"))

        assertThrows<IllegalStateException> { client.complete("프롬프트") }
    }

    @Test
    fun `제공자가 없으면 available=false이고 호출 시 예외가 발생한다`() {
        val empty = LlmClient(emptyList())

        assertFalse(empty.available)
        assertThrows<IllegalStateException> { empty.complete("프롬프트") }
    }

    @Test
    fun `제공자가 있으면 available=true다`() {
        assertTrue(client.available)
        assertEquals(listOf("gemini", "groq"), client.providerNames)
    }
}
