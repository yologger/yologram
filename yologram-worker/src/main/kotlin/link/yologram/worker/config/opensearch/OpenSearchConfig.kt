package link.yologram.worker.config.opensearch

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.opensearch.OpenSearchClient
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

    @Bean(destroyMethod = "close")
    fun openSearchClient(properties: OpenSearchProperties): OpenSearchClient {
        val uri = URI(properties.uri)
        val host = HttpHost(uri.scheme, uri.host, if (uri.port == -1) defaultPort(uri.scheme) else uri.port)

        val credentialsProvider = BasicCredentialsProvider().apply {
            setCredentials(
                AuthScope(host),
                UsernamePasswordCredentials(properties.username, properties.password.toCharArray()),
            )
        }

        val transport = ApacheHttpClient5TransportBuilder.builder(host)
            .setHttpClientConfigCallback { builder ->
                builder.setDefaultCredentialsProvider(credentialsProvider)
            }
            .build()

        return OpenSearchClient(transport)
    }

    private fun defaultPort(scheme: String) = if (scheme == "https") 443 else 80
}
