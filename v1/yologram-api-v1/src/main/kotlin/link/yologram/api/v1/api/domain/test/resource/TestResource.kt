package link.yologram.api.v1.api.domain.test.resource

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestResource {

    @GetMapping("/test")
    fun test(): String {
        return "test"
    }
}