package link.yologram.worker.global.discord

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "yologram.discord")
data class DiscordProperties(
    // 채널 키 → 웹훅 설정 (tech/politics/invest ... 채널 추가 = 항목 추가, 코드 변경 없음)
    val webhooks: Map<String, Webhook> = emptyMap(),
    val connectTimeoutMillis: Int = 5_000,
    val readTimeoutMillis: Long = 10_000,
    val writeTimeoutMillis: Long = 10_000,
) {
    data class Webhook(
        val url: String = "",
        val enabled: Boolean = false,
    )
}
