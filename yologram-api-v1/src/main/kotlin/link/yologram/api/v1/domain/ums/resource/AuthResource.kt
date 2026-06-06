package link.yologram.api.v1.domain.ums.resource

import jakarta.validation.Valid
import link.yologram.api.v1.domain.ums.model.LoginRequest
import link.yologram.api.v1.domain.ums.model.LoginResponse
import link.yologram.api.v1.domain.ums.model.ValidateTokenResponse
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUser
import link.yologram.api.v1.domain.ums.service.AuthService
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ums/auth")
class AuthResource(
    private val authService: AuthService,
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiEnvelop<LoginResponse> {
        return ApiEnvelop(data = authService.login(request))
    }

    @PostMapping("/validate-token")
    fun validateToken(@AuthenticatedUser authData: AuthData): ApiEnvelop<ValidateTokenResponse> {
        return ApiEnvelop(data = authService.validateToken(authData.accessToken))
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@AuthenticatedUser authData: AuthData) {
        authService.logout(authData.uid)
    }
}
