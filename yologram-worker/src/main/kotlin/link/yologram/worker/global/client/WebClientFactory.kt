package link.yologram.worker.global.client

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.util.concurrent.TimeUnit

/** 워커 공용 WebClient 생성 (bun-client-kotlin WebClientFactory 미러 — 타임아웃·리다이렉트 일원화) */
object WebClientFactory {

    fun create(
        connectTimeoutMillis: Int = 5_000,
        readTimeoutMillis: Long = 10_000,
        writeTimeoutMillis: Long = 10_000,
        followRedirect: Boolean = true,
        customizer: WebClient.Builder.() -> Unit = {},
    ): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
            .followRedirect(followRedirect)
            .doOnConnected { connection ->
                connection
                    .addHandlerLast(ReadTimeoutHandler(readTimeoutMillis, TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(writeTimeoutMillis, TimeUnit.MILLISECONDS))
            }
            .compress(true)

        val builder = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
        builder.customizer()
        return builder.build()
    }
}
