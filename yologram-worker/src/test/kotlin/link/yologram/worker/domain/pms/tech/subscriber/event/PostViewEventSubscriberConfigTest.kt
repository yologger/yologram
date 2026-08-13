package link.yologram.worker.domain.pms.tech.subscriber.event

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.worker.domain.pms.tech.service.TechPostViewIngestService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Consumer

/**
 * 소비 활성화 게이트 검증 — 함수 빈이 없으면 바인딩도 생기지 않으므로
 * "로컬·테스트 기본 비활성"이 프로퍼티 하나로 보장되는지 계약으로 고정한다.
 */
class PostViewEventSubscriberConfigTest {

    @Configuration
    class StubBeans {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()

        @Bean
        fun ingestService(): TechPostViewIngestService = mock()

        @Bean
        fun postViewEventHandler(
            objectMapper: ObjectMapper,
            ingestService: TechPostViewIngestService,
        ) = PostViewEventSubscriber(objectMapper, ingestService)
    }

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(StubBeans::class.java, PostViewEventSubscriberConfig::class.java)

    @Test
    fun `프로퍼티가 없으면 컨슈머 빈이 만들어지지 않는다 (기본 비활성)`() {
        runner.run { context ->
            assert(context.getBeanNamesForType(Consumer::class.java).isEmpty()) {
                "기본값에서 컨슈머 빈이 생성됨 — 로컬·테스트가 prod 스트림을 소비할 위험"
            }
        }
    }

    @Test
    fun `enabled=false면 컨슈머 빈이 만들어지지 않는다`() {
        runner.withPropertyValues("yologram.events.subscribe.post-view.enabled=false").run { context ->
            assert(context.getBeanNamesForType(Consumer::class.java).isEmpty())
        }
    }

    @Test
    fun `enabled=true면 함수 이름 postViewEventSubscribe로 컨슈머 빈이 등록된다`() {
        runner.withPropertyValues("yologram.events.subscribe.post-view.enabled=true").run { context ->
            // 빈 이름이 spring.cloud.function.definition 값과 일치해야 바인딩이 만들어진다
            assert(context.containsBean("postViewEventSubscribe"))
            assert(context.getBean("postViewEventSubscribe") is Consumer<*>)
        }
    }
}
