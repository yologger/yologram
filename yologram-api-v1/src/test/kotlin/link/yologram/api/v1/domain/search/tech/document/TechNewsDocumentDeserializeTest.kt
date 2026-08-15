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
import kotlin.test.assertTrue

/**
 * 색인 문서 역직렬화 계약 — worker가 색인한 JSON을 그대로 읽을 수 있는지 고정한다.
 * (게시글 TechPostDocumentDeserializeTest와 같은 이유 — 조건부 빈이라 통합 테스트가 못 잡는다)
 */
class TechNewsDocumentDeserializeTest {

    /** worker가 실제로 색인하는 문서 형태 (prod에서 조회한 _source) */
    private val indexedJson = """
        {
          "id": 900,
          "title": "Amazon Nova Multimodal Embeddings is now available in AWS GovCloud (US-West)",
          "summary": "**📌 한 줄 요약**\nAWS GovCloud에서 Amazon Nova Multimodal Embeddings를 쓸 수 있다.",
          "link": "https://aws.amazon.com/about-aws/whats-new/2026/08/amazon-nova-mme-govcloud/",
          "sourceName": "AWS What's New",
          "categoryIds": [2, 3, 5],
          "publishedAt": "2026-08-12T23:34:00",
          "createdAt": "2026-08-14T11:10:00"
        }
    """.trimIndent()

    /** Spring Boot가 구성하는 ObjectMapper와 같은 조건 */
    private fun bootObjectMapper(): ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun deserialize(mapper: ObjectMapper): TechNewsDocument {
        val jsonpMapper = JacksonJsonpMapper(mapper)
        jsonpMapper.jsonProvider().createParser(StringReader(indexedJson)).use { parser ->
            return jsonpMapper.deserialize(parser, TechNewsDocument::class.java)
        }
    }

    @Test
    fun `Boot ObjectMapper로 색인 문서를 읽는다`() {
        val doc = deserialize(bootObjectMapper())

        assertEquals(900, doc.id)
        assertTrue(doc.title.startsWith("Amazon Nova"))
        assertTrue(doc.summary.contains("한 줄 요약"))
        assertEquals("AWS What's New", doc.sourceName)
        assertEquals(listOf(2L, 3L, 5L), doc.categoryIds)
        // 최신순 정렬 기준은 수집 시각이 아니라 발행 시각이다
        assertEquals(LocalDateTime.of(2026, 8, 12, 23, 34, 0), doc.publishedAt)
        assertEquals(LocalDateTime.of(2026, 8, 14, 11, 10, 0), doc.createdAt)
    }

    @Test
    fun `기본 ObjectMapper로는 LocalDateTime 역직렬화가 실패한다`() {
        // transport에 mapper를 넘기지 않으면 이 조건이 된다 — 반드시 setMapper로 주입할 것
        assertFailsWith<Exception> { deserialize(ObjectMapper()) }
    }

    @Test
    fun `카테고리가 없는 뉴스도 읽는다`() {
        // LLM 분류가 마스터에 없는 라벨만 뽑으면 매핑이 비어 색인 문서에도 빈 배열이 들어간다
        val jsonpMapper = JacksonJsonpMapper(bootObjectMapper())
        val json = """{"id":1,"title":"제목","summary":"요약","link":"https://news.test/1","sourceName":"GeekNews","categoryIds":[],"publishedAt":"2026-08-12T23:34:00"}"""

        jsonpMapper.jsonProvider().createParser(StringReader(json)).use { parser ->
            val doc = jsonpMapper.deserialize(parser, TechNewsDocument::class.java)
            assertTrue(doc.categoryIds.isEmpty())
            assertEquals("제목", doc.title)
        }
    }
}
