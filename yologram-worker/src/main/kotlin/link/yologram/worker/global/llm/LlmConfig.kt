package link.yologram.worker.global.llm

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ReactorClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * OpenAI 호환 엔드포인트로 Gemini/Groq ChatModel 구성.
 * api-key가 비어 있는 제공자는 제외 — 키가 하나도 없으면 LlmClient.available=false (요약 스텝이 스킵).
 */
@Configuration
@EnableConfigurationProperties(LlmProperties::class)
class LlmConfig {

    @Bean
    fun llmClient(properties: LlmProperties): LlmClient {
        val providers = buildList {
            properties.gemini.takeIf { it.apiKey.isNotBlank() }
                ?.let { add(LlmProvider("gemini", chatModel(it, properties))) }
            properties.groq.takeIf { it.apiKey.isNotBlank() }
                ?.let { add(LlmProvider("groq", chatModel(it, properties))) }
        }
        return LlmClient(providers)
    }

    private fun chatModel(provider: LlmProperties.Provider, properties: LlmProperties): ChatModel {
        // 기본 read timeout(10초)은 thinking 모델의 생성 시간에 부족 — 넉넉히 설정
        val requestFactory = ReactorClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMillis))
            setReadTimeout(Duration.ofMillis(properties.readTimeoutMillis))
        }

        val api = OpenAiApi.builder()
            .baseUrl(provider.baseUrl)
            .apiKey(provider.apiKey)
            .completionsPath(provider.completionsPath)
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
            .build()

        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model(provider.model)
                    .temperature(0.3)
                    .build()
            )
            .build()
    }
}
