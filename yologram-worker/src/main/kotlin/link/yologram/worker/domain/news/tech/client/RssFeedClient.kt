package link.yologram.worker.domain.news.tech.client

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.time.ZoneId

/** RSS 피드에서 수집한 기사 1건 — 저장 전 중간 표현 */
data class CollectedNews(
    val title: String,
    val link: String,
    val publishedAt: LocalDateTime,
)

@Component
class RssFeedClient(
    @Qualifier("outboundWebClient") private val webClient: WebClient,
) {

    fun fetch(feedUrl: String): List<CollectedNews> {
        // 바이트로 받아 XmlReader에 넘김 — XML 선언/헤더 기반 인코딩 자동 감지 유지
        val body = webClient.get()
            .uri(feedUrl)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()
            ?: error("RSS 응답이 비어 있습니다: $feedUrl")
        return parse(body)
    }

    fun parse(body: ByteArray): List<CollectedNews> {
        val feed = ByteArrayInputStream(body).use { SyndFeedInput().build(XmlReader(it)) }

        return feed.entries
            .filter { !it.link.isNullOrBlank() && !it.title.isNullOrBlank() }
            .map { entry ->
                CollectedNews(
                    title = entry.title.trim(),
                    link = entry.link.trim(),
                    // pubDate 없는 피드는 updatedDate → 수집 시각 순으로 폴백
                    publishedAt = (entry.publishedDate ?: entry.updatedDate)
                        ?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
                        ?: LocalDateTime.now(),
                )
            }
    }
}
