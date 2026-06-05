package link.yologram.api.v1.domain.test.resource

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/test")
class TestResource(
    private val environment: Environment
) {
    private val logger = KotlinLogging.logger {}

    @GetMapping
    fun index(): String {
        logger.info { "api.v1.test.index called" }
        return "/v1/test"
    }

    @GetMapping("echo")
    fun echo(request: HttpServletRequest): Map<String, Any?> {
        logger.info {
            "api.v1.test.echo called method=${request.method} uri=${request.requestURI} remoteAddr=${request.remoteAddr}"
        }
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
        val activeProfiles = environment.activeProfiles
        logger.info { "api.v1.test.profile called activeProfiles=${activeProfiles.joinToString(",")}" }
        return activeProfiles
    }

    @GetMapping("property")
    fun getProperty(@RequestParam("key") key: String): String? {
        logger.info { "api.v1.test.property called key=$key" }
        return environment.getProperty(key)
    }

}
