package link.yologram.worker.global.discord

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordNotifierTest {

    private val properties = DiscordProperties(
        webhooks = mapOf(
            "tech" to DiscordProperties.Webhook(url = "https://discord.example.com/webhook", enabled = true),
            "disabled-channel" to DiscordProperties.Webhook(url = "https://discord.example.com/webhook2", enabled = false),
        )
    )

    private fun notifierCountingRequests(counter: AtomicInteger, status: HttpStatus = HttpStatus.NO_CONTENT): DiscordNotifier {
        val exchange = ExchangeFunction {
            counter.incrementAndGet()
            Mono.just(ClientResponse.create(status).build())
        }
        return DiscordNotifier(WebClient.builder().exchangeFunction(exchange).build(), properties)
    }

    @Test
    fun `메시지를 웹훅으로 1회 발송한다`() {
        val counter = AtomicInteger()

        notifierCountingRequests(counter).send("tech", "새 테크 아티클 1건")

        assertEquals(1, counter.get())
    }

    @Test
    fun `빈 메시지는 발송하지 않는다`() {
        val counter = AtomicInteger()

        notifierCountingRequests(counter).send("tech", "  ")

        assertEquals(0, counter.get())
    }

    @Test
    fun `2000자 초과 메시지는 분할 발송한다`() {
        val counter = AtomicInteger()
        val content = (1..300).joinToString("\n") { "라인 $it - 0123456789" } // 2,000자 훌쩍 초과

        notifierCountingRequests(counter).send("tech", content)

        assertTrue(counter.get() >= 2)
    }

    @Test
    fun `발송 실패해도 예외를 전파하지 않는다`() {
        val exchange = ExchangeFunction { Mono.error(RuntimeException("connection refused")) }
        val notifier = DiscordNotifier(WebClient.builder().exchangeFunction(exchange).build(), properties)

        notifier.send("tech", "실패해도 안전") // 예외 없이 통과하면 성공
    }

    @Test
    fun `미등록 채널이면 발송하지 않는다`() {
        val counter = AtomicInteger()

        notifierCountingRequests(counter).send("politics", "미등록 채널")

        assertEquals(0, counter.get())
    }

    @Test
    fun `비활성(enabled=false) 채널이면 발송하지 않는다`() {
        val counter = AtomicInteger()

        notifierCountingRequests(counter).send("disabled-channel", "비활성 채널")

        assertEquals(0, counter.get())
    }

    @Test
    fun `embed를 웹훅으로 1회 발송한다`() {
        val counter = AtomicInteger()

        notifierCountingRequests(counter).sendEmbed(
            channel = "tech",
            title = "코루틴 딥다이브",
            url = "https://tech.example.com/posts/1",
            description = "요약 내용",
            sourceName = "테크 블로그",
        )

        assertEquals(1, counter.get())
    }

    @Test
    fun `embed 발송 실패해도 예외를 전파하지 않는다`() {
        val exchange = ExchangeFunction { Mono.error(RuntimeException("connection refused")) }
        val notifier = DiscordNotifier(WebClient.builder().exchangeFunction(exchange).build(), properties)

        notifier.sendEmbed(channel = "tech", title = "t", url = "https://a/1", description = "d") // 예외 없이 통과하면 성공
    }

    @Test
    fun `splitMessage는 2000자 이하면 그대로 반환한다`() {
        val chunks = DiscordNotifier.splitMessage("짧은 메시지")

        assertEquals(listOf("짧은 메시지"), chunks)
    }

    @Test
    fun `splitMessage는 줄 단위로 분할하며 각 청크가 한도를 넘지 않는다`() {
        val content = (1..300).joinToString("\n") { "라인 $it - 0123456789" }

        val chunks = DiscordNotifier.splitMessage(content)

        assertTrue(chunks.size >= 2)
        assertTrue(chunks.all { it.length <= DiscordNotifier.MAX_CONTENT_LENGTH })
        // 내용 유실 없음 (개행 재조합하면 원본과 동일)
        assertEquals(content, chunks.joinToString("\n"))
    }

    @Test
    fun `splitMessage는 한 줄이 한도를 넘으면 그 줄을 절단한다`() {
        val longLine = "a".repeat(DiscordNotifier.MAX_CONTENT_LENGTH + 500)

        val chunks = DiscordNotifier.splitMessage(longLine)

        assertTrue(chunks.all { it.length <= DiscordNotifier.MAX_CONTENT_LENGTH })
    }
}
