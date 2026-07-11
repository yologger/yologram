package link.yologram.worker.config

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component

@Component
class OpenTelemetryLoggingConfig(
    private val openTelemetrySdk: OpenTelemetrySdk
) : InitializingBean {
    override fun afterPropertiesSet() {
        OpenTelemetryAppender.install(openTelemetrySdk)
    }
}
