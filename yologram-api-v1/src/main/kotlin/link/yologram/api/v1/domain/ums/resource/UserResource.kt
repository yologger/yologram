package link.yologram.api.v1.domain.ums.resource

import jakarta.validation.Valid
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.model.JoinResponse
import link.yologram.api.v1.domain.ums.service.UserService
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ums/user")
class UserResource(
    private val userService: UserService,
) {

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    fun join(@Valid @RequestBody request: JoinRequest): ApiEnvelop<JoinResponse> {
        return ApiEnvelop(data = userService.join(request))
    }
}
