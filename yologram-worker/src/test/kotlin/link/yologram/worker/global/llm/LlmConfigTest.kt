package link.yologram.worker.global.llm

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LlmConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(LlmConfig::class.java))
        .withPropertyValues(
            "yologram.llm.gemini.base-url=https://generativelanguage.googleapis.com/v1beta/openai",
            "yologram.llm.gemini.completions-path=/chat/completions",
            "yologram.llm.gemini.model=gemini-3.5-flash",
            "yologram.llm.groq.base-url=https://api.groq.com/openai",
            "yologram.llm.groq.model=meta-llama/llama-4-scout-17b-16e-instruct",
        )

    @Test
    fun `키가 둘 다 있으면 gemini, groq 순서로 구성된다`() {
        contextRunner
            .withPropertyValues(
                "yologram.llm.gemini.api-key=gemini-key",
                "yologram.llm.groq.api-key=groq-key",
            )
            .run { context ->
                val client = context.getBean(LlmClient::class.java)
                assertEquals(listOf("gemini", "groq"), client.providerNames)
            }
    }

    @Test
    fun `gemini 키만 있으면 gemini만 구성된다`() {
        contextRunner
            .withPropertyValues("yologram.llm.gemini.api-key=gemini-key")
            .run { context ->
                val client = context.getBean(LlmClient::class.java)
                assertEquals(listOf("gemini"), client.providerNames)
            }
    }

    @Test
    fun `키가 없으면 available=false로 구성된다`() {
        contextRunner.run { context ->
            val client = context.getBean(LlmClient::class.java)
            assertFalse(client.available)
        }
    }
}
