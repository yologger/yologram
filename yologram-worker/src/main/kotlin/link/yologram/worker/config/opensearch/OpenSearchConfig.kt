package link.yologram.worker.config.opensearch

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.core5.http.HttpHost
import com.fasterxml.jackson.databind.ObjectMapper
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

/**
 * OpenSearch 클라이언트 수동 빈.
 *
 * enabled=true일 때만 만든다 — 자격증명이 없는 로컬·테스트에서 빈 생성이 실패하거나
 * prod 인덱스에 붙는 것을 막는다(RedisConfig·PostViewEventSubscriberConfig와 같은 판단).
 *
 * Caddy가 정식 인증서로 TLS를 종료하므로 클라이언트는 검증을 끄지 않는다 —
 * self-signed 예외는 컨테이너 내부(Dashboards→OpenSearch) 구간에만 있다.
 */
@Configuration
@EnableConfigurationProperties(OpenSearchProperties::class)
@ConditionalOnProperty(prefix = "opensearch.main", name = ["enabled"], havingValue = "true")
class OpenSearchConfig {

    /**
     * 커넥션 풀을 쥐고 있는 쪽은 transport다 — 클라이언트가 아니라 여기에 destroyMethod를 건다.
     * OpenSearchClient에는 close()가 없어 클라이언트 빈에 걸면 기동 시점에
     * "Could not find a destroy method named 'close'"로 컨텍스트가 죽는다.
     */
    @Bean(destroyMethod = "close")
    fun openSearchTransport(properties: OpenSearchProperties, objectMapper: ObjectMapper): OpenSearchTransport {
        val uri = URI(properties.uri)
        val host = HttpHost(uri.scheme, uri.host, if (uri.port == -1) defaultPort(uri.scheme) else uri.port)

        val credentialsProvider = BasicCredentialsProvider().apply {
            setCredentials(
                AuthScope(host),
                UsernamePasswordCredentials(properties.username, properties.password.toCharArray()),
            )
        }

        return ApacheHttpClient5TransportBuilder.builder(host)
            .setHttpClientConfigCallback { builder ->
                builder.setDefaultCredentialsProvider(credentialsProvider)
            }
            // Spring Boot가 구성한 ObjectMapper를 넘긴다 — JacksonJsonpMapper의 기본 ObjectMapper는
            // Java 8 날짜 타입을 모르는 상태(공식 USER_GUIDE: "by default supports Java 7 objects")라
            // LocalDateTime을 담은 문서를 색인하는 순간 직렬화가 실패한다.
            // Boot의 것은 JavaTimeModule이 등록되고 WRITE_DATES_AS_TIMESTAMPS가 꺼져 있어
            // 매핑의 date_optional_time과 맞는 ISO-8601 문자열로 나간다
            .setMapper(JacksonJsonpMapper(objectMapper))
            .build()
    }

    @Bean
    fun openSearchClient(transport: OpenSearchTransport): OpenSearchClient = OpenSearchClient(transport)

    private fun defaultPort(scheme: String) = if (scheme == "https") 443 else 80
}
