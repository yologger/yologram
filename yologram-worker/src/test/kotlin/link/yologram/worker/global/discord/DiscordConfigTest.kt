package link.yologram.worker.global.discord

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DiscordConfig::class.java))

    @Test
    fun `DiscordNotifier 빈은 항상 등록된다`() {
        contextRunner.run { context ->
            assertTrue(context.containsBean("discordNotifier"))
        }
    }

    @Test
    fun `채널별 웹훅 설정이 바인딩된다`() {
        contextRunner
            .withPropertyValues(
                "yologram.webhooks.discord.tech-news.url=https://discord.example.com/webhook",
                "yologram.webhooks.discord.tech-news.enabled=true",
                "yologram.webhooks.discord.politics-news.url=https://discord.example.com/webhook2",
                "yologram.webhooks.discord.politics-news.enabled=false",
            )
            .run { context ->
                val properties = context.getBean(DiscordProperties::class.java)
                assertEquals(2, properties.discord.size)
                assertEquals("https://discord.example.com/webhook", properties.discord["tech-news"]!!.url)
                assertTrue(properties.discord["tech-news"]!!.enabled)
                assertEquals(false, properties.discord["politics-news"]!!.enabled)
            }
    }
}
