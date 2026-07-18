package link.yologram.worker.global.llm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "yologram.llm")
data class LlmProperties(
    val gemini: Provider = Provider(),
    val groq: Provider = Provider(),
    val connectTimeoutMillis: Long = 5_000,
    // LLM 생성 시간 고려 — thinking 모델은 긴 본문에 10~20초+ 소요 (기본 10초로는 타임아웃)
    val readTimeoutMillis: Long = 60_000,
) {
    data class Provider(
        // 비어 있으면 해당 제공자 비활성 (api-key는 Parameter Store 주입)
        val apiKey: String = "",
        val baseUrl: String = "",
        val completionsPath: String = "/v1/chat/completions",
        val model: String = "",
    )
}
