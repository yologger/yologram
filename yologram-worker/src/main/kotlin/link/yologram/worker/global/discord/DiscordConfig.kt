package link.yologram.worker.global.discord

import link.yologram.worker.global.client.WebClientFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * DiscordNotifier 빈은 항상 등록 — 발송 여부는 채널별 설정(webhooks.{channel}.enabled/url)이 결정.
 * 미등록·비활성 채널로의 발송은 Notifier가 스킵.
 */
@Configuration
@EnableConfigurationProperties(DiscordProperties::class)
class DiscordConfig {

    @Bean
    fun discordNotifier(properties: DiscordProperties): DiscordNotifier {
        val webClient = WebClientFactory.create(
            connectTimeoutMillis = properties.connectTimeoutMillis,
            readTimeoutMillis = properties.readTimeoutMillis,
            writeTimeoutMillis = properties.writeTimeoutMillis,
        )
        return DiscordNotifier(webClient, properties)
    }
}
