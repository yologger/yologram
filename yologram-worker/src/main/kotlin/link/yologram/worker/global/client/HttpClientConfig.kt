package link.yologram.worker.global.client

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class HttpClientConfig {

    /** 외부 콘텐츠 수집용 공용 WebClient (RSS 피드·기사 원문 — 리다이렉트 추적, 수집 UA) */
    @Bean
    fun outboundWebClient(): WebClient = WebClientFactory.create {
        defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
        // RSS/HTML 응답이 큰 경우 대비 (기본 256KB → 4MB)
        codecs { it.defaultCodecs().maxInMemorySize(4 * 1024 * 1024) }
    }

    companion object {
        const val USER_AGENT = "yologram-worker/1.0 (+https://yologram.link)"
    }
}
