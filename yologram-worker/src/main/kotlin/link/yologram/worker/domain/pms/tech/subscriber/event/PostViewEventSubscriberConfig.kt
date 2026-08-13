package link.yologram.worker.domain.pms.tech.subscriber.event

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
@ConditionalOnProperty(prefix = "yologram.events.subscribe.post-view", name = ["enabled"], havingValue = "true")
class PostViewEventSubscriberConfig {

    @Bean
    fun postViewEventSubscribe(subscriber: PostViewEventSubscriber): Consumer<Message<List<*>>> =
        Consumer { message -> subscriber.handle(message) }
}
