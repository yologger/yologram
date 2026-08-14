package link.yologram.api.v1.config.sqs

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 메시지 발행(SQS) 설정 — EventPublishProperties(Kinesis)와 같은 형태의 형제 축이다.
 *
 * 수단으로 축을 가른다(docs/rules.md): events=스트림(Kinesis), messages=큐(SQS).
 * worker의 구독 설정(yologram.messages.subscribe.{메시지})과 대칭이다.
 *
 * enabled=false(로컬·테스트 기본)거나 queue가 비어 있으면 발행을 스킵한다 —
 * 로컬에서 prod 큐에 인덱싱 작업을 넣어 실인덱스를 건드리는 사고를 막는다.
 */
@ConfigurationProperties(prefix = "yologram.messages.publish")
data class MessagePublishProperties(
    val postIndex: Publish = Publish(),
) {
    data class Publish(
        val enabled: Boolean = false,
        val queue: String? = null,
    )
}
