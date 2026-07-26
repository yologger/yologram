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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewsContentCrawlerTest {

    private val url = "https://tech.example.com/posts/1"

    private fun fixtureHtml() = javaClass.getResource("/html/sample-news.html")!!.readText()

    private fun crawlerRespondingWith(status: HttpStatus, body: String? = null): NewsContentCrawler {
        val exchange = ExchangeFunction {
            val builder = ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
            body?.let { builder.body(it) }
            Mono.just(builder.build())
        }
        return NewsContentCrawler(WebClient.builder().exchangeFunction(exchange).build())
    }

    private val crawler = crawlerRespondingWith(HttpStatus.OK)

    @Test
    fun `원문을 로드해 본문 텍스트를 추출한다`() {
        val text = crawlerRespondingWith(HttpStatus.OK, fixtureHtml()).fetch(url)

        assertTrue(text.contains("코루틴은 코틀린의 대표적인 비동기 처리 도구입니다"))
        assertTrue(text.contains("구조화된 동시성"))
    }

    @Test
    fun `내비게이션·푸터 등 본문 외 요소는 제거된다`() {
        val text = crawler.extract(url, fixtureHtml())

        assertFalse(text.contains("아카이브"))
        assertFalse(text.contains("무단 전재 금지"))
    }

    @Test
    fun `연속 공백과 개행은 한 칸으로 정규화된다`() {
        val text = crawler.extract(url, fixtureHtml())

        assertFalse(text.contains("\n"))
        assertFalse(text.contains("  "))
    }

    @Test
    fun `본문이 한도를 넘으면 절단한다`() {
        val longParagraph = "<p>${"가나다라마바사 ".repeat(5_000)}</p>"
        val html = "<html><body><article>$longParagraph</article></body></html>"

        val text = crawler.extract(url, html)

        assertEquals(NewsContentCrawler.MAX_CONTENT_CHARS, text.length)
    }

    @Test
    fun `본문을 추출할 수 없으면 예외가 발생한다`() {
        assertThrows<IllegalStateException> {
            crawler.extract(url, "<html><body></body></html>")
        }
    }

    @Test
    fun `HTTP 오류 응답이면 예외가 발생한다`() {
        assertThrows<WebClientResponseException> {
            crawlerRespondingWith(HttpStatus.FORBIDDEN).fetch(url)
        }
    }

    @Test
    fun `빈 응답이면 예외가 발생한다`() {
        assertThrows<IllegalStateException> {
            crawlerRespondingWith(HttpStatus.OK).fetch(url)
        }
    }
}
