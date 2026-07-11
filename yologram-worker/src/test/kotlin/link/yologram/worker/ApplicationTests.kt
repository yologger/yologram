package link.yologram.worker

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationTests {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `컨텍스트가 정상 기동된다`() {
        // @SpringBootTest 로드 자체가 검증 — 별도 단언 불필요
    }

    @Test
    fun `actuator health가 UP을 반환한다`() {
        val response = restTemplate.getForEntity("http://localhost:$port/actuator/health", String::class.java)

        assertEquals(200, response.statusCode.value())
        assertTrue(response.body!!.contains("\"status\":\"UP\""))
    }
}
