package link.yologram.worker.global.llm

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel

private val logger = KotlinLogging.logger {}

data class LlmProvider(val name: String, val chatModel: ChatModel)

data class LlmCompletion(val provider: String, val content: String)

/**
 * 우선순위 순서의 제공자 목록으로 fallback 호출 (Gemini → Groq).
 * 앞 제공자가 실패(429/5xx/네트워크)하면 다음 제공자로 넘어가고, 전부 실패하면 예외.
 */
class LlmClient(private val providers: List<LlmProvider>) {

    val available: Boolean
        get() = providers.isNotEmpty()

    val providerNames: List<String>
        get() = providers.map { it.name }

    fun complete(prompt: String): LlmCompletion {
        check(providers.isNotEmpty()) { "구성된 LLM 제공자가 없습니다" }

        val failures = mutableListOf<String>()
        for (provider in providers) {
            runCatching { provider.chatModel.call(prompt) }
                .onSuccess { content ->
                    if (!content.isNullOrBlank()) return LlmCompletion(provider.name, content.trim())
                    logger.warn { "LLM 빈 응답: provider=${provider.name}" }
                    failures += "${provider.name}: 빈 응답"
                }
                .onFailure {
                    logger.warn(it) { "LLM 호출 실패: provider=${provider.name}" }
                    failures += "${provider.name}: ${it.message}"
                }
        }
        error("모든 LLM 제공자 호출 실패: $failures")
    }
}
