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
                "yologram.discord.webhooks.tech.url=https://discord.example.com/webhook",
                "yologram.discord.webhooks.tech.enabled=true",
                "yologram.discord.webhooks.politics.url=https://discord.example.com/webhook2",
                "yologram.discord.webhooks.politics.enabled=false",
            )
            .run { context ->
                val properties = context.getBean(DiscordProperties::class.java)
                assertEquals(2, properties.webhooks.size)
                assertEquals("https://discord.example.com/webhook", properties.webhooks["tech"]!!.url)
                assertTrue(properties.webhooks["tech"]!!.enabled)
                assertEquals(false, properties.webhooks["politics"]!!.enabled)
            }
    }
}
