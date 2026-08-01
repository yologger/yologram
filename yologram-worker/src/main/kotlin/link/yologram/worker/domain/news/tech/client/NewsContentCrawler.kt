package link.yologram.worker.domain.news.tech.client

import net.dankito.readability4j.Readability4J
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class NewsContentCrawler(
    @Qualifier("outboundWebClient") private val webClient: WebClient,
) {

    /** 기사 원문을 로드해 본문 텍스트만 추출 (LLM 요약 입력) */
    fun fetch(url: String): String {
        val html = webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
            ?: error("원문 응답이 비어 있습니다: $url")
        return extract(url, html)
    }

    /** Readability로 광고·내비 등 제거 후 본문 텍스트 추출. 요약 입력 한도로 절단 */
    fun extract(url: String, html: String): String {
        val parsed = Readability4J(url, html).parse()
        val text = parsed.textContent
            ?.replace(WHITESPACE, " ")
            ?.trim()
        check(!text.isNullOrBlank()) { "본문 추출 실패: $url" }
        return text.take(MAX_CONTENT_CHARS)
    }

    companion object {
        // LLM 입력 한도 — 블로그 본문 전체를 요약에 다 쓸 필요는 없음
        const val MAX_CONTENT_CHARS = 20_000
        private val WHITESPACE = Regex("\\s+")
    }
}
