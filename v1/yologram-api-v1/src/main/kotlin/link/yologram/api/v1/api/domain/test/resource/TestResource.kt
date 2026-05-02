package link.yologram.api.v1.api.domain.test.resource

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/test")
class TestResource {

    @GetMapping
    fun index(): String {
        return "/v1/test"
    }
}