package link.yologram.worker.global.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.reactive.function.client.WebClient

private val logger = KotlinLogging.logger {}

class DiscordNotifier(
    private val webClient: WebClient,
    private val properties: DiscordProperties,
) {

    /**
     * 채널 웹훅으로 메시지 발송. Discord content 한도(2,000자) 초과 시 줄 단위로 분할 발송.
     * 알림은 부가 기능 — 실패해도 예외를 전파하지 않고 로그만 남긴다.
     */
    fun send(channel: String, content: String) {
        if (content.isBlank()) return

        splitMessage(content).forEach { chunk ->
            post(channel, mapOf("content" to chunk))
        }
    }

    /** embed 발송 — 상단에 소스명, 제목이 원문 링크로 걸리고 본문에 요약 (n8n 알림 포맷 대체) */
    fun sendEmbed(channel: String, title: String, url: String, description: String, sourceName: String? = null) {
        val embed = buildMap {
            put("title", title.take(MAX_EMBED_TITLE_LENGTH))
            put("url", url)
            put("description", description.take(MAX_EMBED_DESCRIPTION_LENGTH))
            put("color", EMBED_COLOR)
        }
        val body = buildMap<String, Any> {
            sourceName?.let { put("content", "📨 **$it**") }
            put("embeds", listOf(embed))
        }
        post(channel, body)
    }

    private fun post(channel: String, body: Map<String, Any>) {
        val webhook = properties.discord[channel]
        if (webhook == null || !webhook.enabled) {
            // 미등록/비활성 채널은 의도된 상태일 수 있어 debug만
            logger.debug { "Discord 채널 비활성 — 발송 스킵: channel=$channel" }
            return
        }
        if (webhook.url.isBlank()) {
            logger.warn { "Discord 채널 활성인데 url 미설정 — 발송 스킵: channel=$channel (yologram.discord.webhooks.$channel.url 확인)" }
            return
        }

        runCatching {
            webClient.post()
                .uri(webhook.url)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block()
        }.onFailure {
            logger.error(it) { "Discord 알림 발송 실패: channel=$channel" }
        }
    }

    companion object {
        // 채널 키 (yologram.discord.webhooks.* 와 일치)
        const val CHANNEL_TECH = "tech-news"

        const val MAX_CONTENT_LENGTH = 2_000
        const val MAX_EMBED_TITLE_LENGTH = 256
        const val MAX_EMBED_DESCRIPTION_LENGTH = 4_096

        // n8n 알림과 동일한 색 (#009DFF)
        private const val EMBED_COLOR = 0x009DFF

        /** 2,000자 한도에 맞춰 줄 단위 분할. 한 줄이 한도를 넘으면 그 줄 자체를 절단 */
        fun splitMessage(content: String): List<String> {
            if (content.length <= MAX_CONTENT_LENGTH) return listOf(content)

            val chunks = mutableListOf<String>()
            val current = StringBuilder()
            content.lineSequence().forEach { line ->
                val trimmedLine = if (line.length > MAX_CONTENT_LENGTH) line.take(MAX_CONTENT_LENGTH) else line
                if (current.isNotEmpty() && current.length + trimmedLine.length + 1 > MAX_CONTENT_LENGTH) {
                    chunks.add(current.toString())
                    current.clear()
                }
                if (current.isNotEmpty()) current.append('\n')
                current.append(trimmedLine)
            }
            if (current.isNotEmpty()) chunks.add(current.toString())
            return chunks
        }
    }
}
