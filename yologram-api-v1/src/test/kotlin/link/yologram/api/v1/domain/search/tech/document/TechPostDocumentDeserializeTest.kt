package link.yologram.api.v1.domain.search.tech.document

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import java.io.StringReader
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 색인 문서 역직렬화 계약 — worker가 색인한 JSON을 그대로 읽을 수 있는지 고정한다.
 *
 * OpenSearchConfig가 조건부 빈(opensearch.main.enabled)이라 통합 테스트에서는 만들어지지 않는다.
 * worker에서 같은 이유로 mapper 문제를 배포 후에야 발견했으므로(docs/done.md) 여기서 단위로 잡는다.
 */
class TechPostDocumentDeserializeTest {

    /** worker가 실제로 색인하는 문서 형태 (prod에서 조회한 _source) */
    private val indexedJson = """
        {
          "id": 1200,
          "uid": 12,
          "title": "쿠쿠쿠",
          "content": "쿠쿠쿠",
          "categoryIds": [2],
          "metrics": { "commentCount": 2, "likeCount": 1, "viewCount": 2 },
          "createdAt": "2026-07-18T14:23:50",
          "modifiedAt": "2026-07-18T14:23:50"
        }
    """.trimIndent()

    /** Spring Boot가 구성하는 ObjectMapper와 같은 조건 */
    private fun bootObjectMapper(): ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun deserialize(mapper: ObjectMapper): TechPostDocument {
        val jsonpMapper = JacksonJsonpMapper(mapper)
        jsonpMapper.jsonProvider().createParser(StringReader(indexedJson)).use { parser ->
            return jsonpMapper.deserialize(parser, TechPostDocument::class.java)
        }
    }

    @Test
    fun `Boot ObjectMapper로 색인 문서를 읽는다`() {
        val doc = deserialize(bootObjectMapper())

        assertEquals(1200, doc.id)
        assertEquals(12, doc.uid)
        assertEquals("쿠쿠쿠", doc.title)
        assertEquals(listOf(2L), doc.categoryIds)
        assertEquals(2, doc.metrics.commentCount)
        assertEquals(1, doc.metrics.likeCount)
        assertEquals(2, doc.metrics.viewCount)
        // 색인 시각은 ISO-8601 문자열 — 매핑이 date_optional_time이라 epoch 숫자가 아니다
        assertEquals(LocalDateTime.of(2026, 7, 18, 14, 23, 50), doc.createdAt)
    }

    @Test
    fun `기본 ObjectMapper로는 LocalDateTime 역직렬화가 실패한다`() {
        // transport에 mapper를 넘기지 않으면 이 조건이 된다 — 반드시 setMapper로 주입할 것
        assertFailsWith<Exception> { deserialize(ObjectMapper()) }
    }

    @Test
    fun `제목이 없는 글도 읽는다`() {
        val jsonpMapper = JacksonJsonpMapper(bootObjectMapper())
        val json = """{"id":1,"uid":12,"content":"본문","categoryIds":[],"metrics":{"commentCount":0,"likeCount":0,"viewCount":0},"createdAt":"2026-07-18T14:23:50"}"""

        jsonpMapper.jsonProvider().createParser(StringReader(json)).use { parser ->
            val doc = jsonpMapper.deserialize(parser, TechPostDocument::class.java)
            assertNull(doc.title)
            assertEquals("본문", doc.content)
        }
    }
}
