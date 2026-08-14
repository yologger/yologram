package link.yologram.api.v1.config.ses

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "yologram.ses")
data class SesProperties(
    val fromAddress: String,
)
