package link.yologram.worker.config.opensearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.json.stream.JsonGenerator
import link.yologram.worker.domain.search.tech.document.TechPostDocument
import org.junit.jupiter.api.Test
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import java.io.StringWriter
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 색인 문서 직렬화 계약 — 배포 후 색인이 조용히 실패한 원인을 고정한다.
 *
 * transport에 mapper를 넘기지 않으면 opensearch-java가 만드는 기본 ObjectMapper가 쓰이는데,
 * 그것은 Java 8 날짜 타입을 모른다(공식 USER_GUIDE: "by default supports Java 7 objects").
 * 게시글이 없는 범위는 직렬화가 일어나지 않아 통과하고, 데이터가 있는 범위만 실패해
 * "큐는 비는데 문서는 0건"으로 보였다.
 */
class OpenSearchJsonMapperTest {

    private fun document() = TechPostDocument(
        id = 1200,
        uid = 12,
        title = "쿠쿠쿠",
        content = "쿠쿠쿠",
        categoryIds = listOf(2),
        metrics = TechPostDocument.Metrics(commentCount = 2, likeCount = 1, viewCount = 2),
        createdAt = LocalDateTime.of(2026, 7, 18, 5, 23, 50),
        modifiedAt = null,
    )

    private fun serialize(mapper: JacksonJsonpMapper): String {
        val writer = StringWriter()
        mapper.jsonProvider().createGenerator(writer).use { generator: JsonGenerator ->
            mapper.serialize(document(), generator)
        }
        return writer.toString()
    }

    /** Spring Boot가 구성하는 ObjectMapper와 같은 조건 */
    private fun bootObjectMapper(): ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `Boot ObjectMapper를 넘기면 날짜가 ISO-8601 문자열로 직렬화된다`() {
        val json = serialize(JacksonJsonpMapper(bootObjectMapper()))

        // 인덱스 매핑이 date_optional_time이라 ISO 문자열이어야 한다 (epoch 숫자면 매핑과 어긋난다)
        assertTrue(json.contains("\"createdAt\":\"2026-07-18T05:23:50\""), json)
        assertTrue(json.contains("\"viewCount\":2"), json)
    }

    @Test
    fun `기본 ObjectMapper로는 LocalDateTime 직렬화가 실패한다`() {
        // transport에 mapper를 넘기지 않으면 이 조건이 된다 — 반드시 setMapper로 주입할 것
        assertFailsWith<Exception> { serialize(JacksonJsonpMapper(ObjectMapper())) }
    }
}
