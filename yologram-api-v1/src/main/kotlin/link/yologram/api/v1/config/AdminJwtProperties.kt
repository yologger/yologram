package link.yologram.api.v1.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "yologram.auth.admin-jwt")
data class AdminJwtProperties(
    val secret: String,
    val expire: Long,
    val issuer: String,
    val audience: String,
)
