package link.yologram.worker.domain.tech.news.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.Month
import kotlin.test.assertEquals

class RssFeedClientTest {

    private fun fixture(path: String) = javaClass.getResourceAsStream(path)!!.readBytes()

    private fun clientRespondingWith(status: HttpStatus, body: ByteArray? = null): RssFeedClient {
        val exchange = ExchangeFunction {
            val builder = ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
            body?.let { builder.body(String(it, Charsets.UTF_8)) }
            Mono.just(builder.build())
        }
        return RssFeedClient(WebClient.builder().exchangeFunction(exchange).build())
    }

    @Test
    fun `RSS 피드를 로드·파싱해 기사 목록을 반환한다`() {
        val client = clientRespondingWith(HttpStatus.OK, fixture("/rss/sample.xml"))

        val newsItems = client.fetch("https://tech.example.com/feed")

        // link 없는 항목은 제외되어 4건
        assertEquals(4, newsItems.size)
        val first = newsItems[0]
        assertEquals("코틀린 코루틴 딥다이브", first.title)
        assertEquals("https://tech.example.com/posts/1", first.link)
        assertEquals(LocalDateTime.of(2026, Month.JULY, 6, 9, 0, 0), first.publishedAt)
    }

    @Test
    fun `pubDate가 없으면 수집 시각으로 폴백한다`() {
        val client = clientRespondingWith(HttpStatus.OK, fixture("/rss/sample.xml"))
        val before = LocalDateTime.now()

        val newsItems = client.fetch("https://tech.example.com/feed")

        val after = LocalDateTime.now()
        val noPubDate = newsItems.first { it.link == "https://tech.example.com/posts/2" }
        kotlin.test.assertFalse(noPubDate.publishedAt.isBefore(before))
        kotlin.test.assertFalse(noPubDate.publishedAt.isAfter(after))
    }

    @Test
    fun `title과 link의 공백을 트림한다`() {
        val newsItems = clientRespondingWith(HttpStatus.OK, fixture("/rss/sample.xml"))
            .fetch("https://tech.example.com/feed")

        val trimmed = newsItems.first { it.link == "https://tech.example.com/posts/3" }
        assertEquals("공백 트림 확인", trimmed.title)
    }

    @Test
    fun `RSS가 아닌 응답이면 예외가 발생한다`() {
        val client = clientRespondingWith(HttpStatus.OK, fixture("/rss/malformed.xml"))

        assertThrows<Exception> { client.fetch("https://tech.example.com/feed") }
    }

    @Test
    fun `HTTP 오류 응답이면 예외가 발생한다`() {
        val client = clientRespondingWith(HttpStatus.NOT_FOUND)

        assertThrows<WebClientResponseException> { client.fetch("https://tech.example.com/feed") }
    }

    @Test
    fun `빈 응답이면 예외가 발생한다`() {
        val client = clientRespondingWith(HttpStatus.OK)

        assertThrows<IllegalStateException> { client.fetch("https://tech.example.com/feed") }
    }
}
