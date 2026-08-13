package link.yologram.api.v1.global.util

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockHttpServletRequest

class ClientIpResolverTest {

    private fun request(
        clientIp: String? = null,
        forwardedFor: String? = null,
        remoteAddr: String = "10.0.0.1",
    ) =
        MockHttpServletRequest().apply {
            clientIp?.let { addHeader("X-Client-Ip", it) }
            forwardedFor?.let { addHeader("X-Forwarded-For", it) }
            this.remoteAddr = remoteAddr
        }

    /** remoteAddr조차 없는 요청 — MockHttpServletRequest는 null remoteAddr을 허용하지 않아 mock으로 대체 */
    private fun requestWithoutRemoteAddr() = mock<HttpServletRequest>()

    @Test
    fun `X-Client-Ip가 있으면 그 값을 쓴다 (API Gateway sourceIp)`() {
        assertEquals("1.2.3.4", ClientIpResolver.resolve(request(clientIp = "1.2.3.4")))
    }

    @Test
    fun `X-Client-Ip는 X-Forwarded-For보다 우선한다`() {
        // 게이트웨이가 overwrite로 넣는 값이라 XFF(클라이언트가 보낼 수 있는 값)보다 신뢰도가 높다
        val resolved = ClientIpResolver.resolve(request(clientIp = "1.2.3.4", forwardedFor = "9.9.9.9"))
        assertEquals("1.2.3.4", resolved)
    }

    @Test
    fun `X-Client-Ip가 빈 값이면 X-Forwarded-For로 폴백한다`() {
        assertEquals("9.9.9.9", ClientIpResolver.resolve(request(clientIp = "   ", forwardedFor = "9.9.9.9")))
    }

    @Test
    fun `X-Client-Ip 앞뒤 공백은 제거한다`() {
        assertEquals("1.2.3.4", ClientIpResolver.resolve(request(clientIp = "  1.2.3.4  ")))
    }

    @Test
    fun `X-Client-Ip도 X-Forwarded-For도 없으면 remoteAddr로 폴백한다`() {
        assertEquals("10.0.0.1", ClientIpResolver.resolve(request()))
    }

    @Test
    fun `X-Forwarded-For가 있으면 그 값을 쓴다`() {
        assertEquals("1.2.3.4", ClientIpResolver.resolve(request(forwardedFor = "1.2.3.4")))
    }

    @Test
    fun `프록시 체인이면 첫 값(원 클라이언트)을 쓴다`() {
        assertEquals("1.2.3.4", ClientIpResolver.resolve(request(forwardedFor = "1.2.3.4, 70.41.3.18, 150.172.238.178")))
    }

    @Test
    fun `첫 값 앞뒤 공백은 제거한다`() {
        assertEquals("1.2.3.4", ClientIpResolver.resolve(request(forwardedFor = "  1.2.3.4 , 70.41.3.18")))
    }

    @Test
    fun `X-Forwarded-For가 없으면 remoteAddr로 폴백한다`() {
        assertEquals("10.0.0.1", ClientIpResolver.resolve(request()))
    }

    @Test
    fun `X-Forwarded-For가 빈 값이면 remoteAddr로 폴백한다`() {
        assertEquals("10.0.0.1", ClientIpResolver.resolve(request(forwardedFor = "   ")))
    }

    @Test
    fun `X-Forwarded-For도 remoteAddr도 없으면 null이다`() {
        assertEquals(null, ClientIpResolver.resolve(requestWithoutRemoteAddr()))
    }

    @Test
    fun `IPv6 주소도 그대로 반환한다`() {
        assertEquals("2001:db8::1", ClientIpResolver.resolve(request(forwardedFor = "2001:db8::1, 70.41.3.18")))
    }
}
