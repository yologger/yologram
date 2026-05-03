package link.yologram.api.v1.api.domain.test.resource

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/test")
class TestResource(
    private val environment: Environment
) {

    @GetMapping
    fun index(): String {
        return "/v1/test"
    }

    @GetMapping("echo")
    fun echo(request: HttpServletRequest): Map<String, Any?> {
        val headers = mutableMapOf<String, String>()
        val headerNames = request.headerNames
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            headers[headerName] = request.getHeader(headerName)
        }

        return mapOf(
            "ip" to request.remoteAddr,
            "userAgent" to request.getHeader("User-Agent"),
            "method" to request.method,
            "uri" to request.requestURI,
            "protocol" to request.protocol,
            "headers" to headers
        )
    }

    @GetMapping("profile")
    fun profile(): Array<String> {
        return environment.activeProfiles
    }
}