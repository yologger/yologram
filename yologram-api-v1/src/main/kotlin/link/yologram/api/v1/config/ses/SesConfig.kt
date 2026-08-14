package link.yologram.api.v1.config.ses

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient

@Configuration
@Profile("prod")
class SesConfig {

    @Bean
    fun sesClient(): SesClient {
        return SesClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .build()
    }
}
