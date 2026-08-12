package link.yologram.api.v1.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 이벤트 스트림(Kinesis) 설정 — CacheRedisProperties와 동일한 패턴(커스텀 프로퍼티 + 수동 빈: KinesisConfig).
 * 스트림 이름은 비밀값이 아니라 고정 이름이므로 Parameter Store가 아닌 yaml에 직접 둔다.
 *
 * name이 비어 있으면 발행을 스킵한다 (로컬·테스트 기본값 — prod 스트림 오염 방지).
 */
@ConfigurationProperties(prefix = "event.stream")
data class EventStreamProperties(
    val postView: Stream = Stream(),
) {
    data class Stream(
        val name: String? = null,
    )
}
