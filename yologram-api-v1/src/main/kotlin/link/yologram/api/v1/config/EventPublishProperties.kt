package link.yologram.api.v1.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 이벤트 발행(Kinesis) 설정 — CacheRedisProperties와 동일한 패턴(커스텀 프로퍼티 + 수동 빈: KinesisConfig).
 * 스트림 이름은 비밀값이 아니라 고정 이름이므로 Parameter Store가 아닌 yaml에 직접 둔다.
 *
 * 경로는 worker의 구독 설정(yologram.events.subscribe.{이벤트})과 대칭이다 —
 * 발행이 늘면 yologram.events.publish.{이벤트}로 형제 항목을 추가한다.
 *
 * enabled=false(로컬·테스트 기본)거나 stream이 비어 있으면 발행을 스킵한다 (prod 스트림 오염 방지).
 */
@ConfigurationProperties(prefix = "yologram.events.publish")
data class EventPublishProperties(
    val postView: Publish = Publish(),
) {
    data class Publish(
        val enabled: Boolean = false,
        val stream: String? = null,
    )
}
