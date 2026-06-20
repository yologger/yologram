package link.yologram.api.v1.domain.ums.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.ums.model.LoginRequest
import link.yologram.api.v1.domain.ums.model.LoginResponse
import link.yologram.api.v1.domain.ums.model.UserEmailVerificationSendRequest
import link.yologram.api.v1.domain.ums.model.ValidateTokenResponse
import link.yologram.api.v1.domain.ums.model.UserEmailVerificationVerifyRequest
import link.yologram.api.v1.domain.ums.model.UserPasswordResetSendRequest
import link.yologram.api.v1.domain.ums.model.UserPasswordResetVerifyRequest
import link.yologram.api.v1.domain.ums.model.UserPasswordResetConfirmRequest
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUser
import link.yologram.api.v1.domain.ums.service.AuthService
import link.yologram.api.v1.domain.ums.service.UserEmailVerificationService
import link.yologram.api.v1.domain.ums.service.UserPasswordResetService
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/v1/ums/auth")
class AuthResource(
    private val authService: AuthService,
    private val emailVerificationService: UserEmailVerificationService,
    private val passwordResetService: UserPasswordResetService,
) {

    @PostMapping("/login")
    @Operation(summary = "로그인")
    fun login(@Valid @RequestBody request: LoginRequest): ApiEnvelop<LoginResponse> {
        return ApiEnvelop(data = authService.login(request))
    }

    @PostMapping("/validate-token")
    @Operation(summary = "토큰 검증")
    fun validateToken(@AuthenticatedUser authData: AuthData): ApiEnvelop<ValidateTokenResponse> {
        return ApiEnvelop(data = authService.validateToken(authData.accessToken))
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "로그아웃")
    fun logout(@AuthenticatedUser authData: AuthData) {
        authService.logout(authData.uid)
    }

    @PostMapping("/email-verification/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "이메일 인증 코드 발송", description = "회원가입 전 이메일 인증 코드를 발송 (5분 유효)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "발송 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "409", description = "이미 가입된 이메일"),
    )
    fun sendCode(@Valid @RequestBody request: UserEmailVerificationSendRequest) {
        emailVerificationService.sendCode(request.email)
    }

    @PostMapping("/email-verification/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "이메일 인증 코드 검증", description = "발송된 인증 코드를 검증")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "인증 성공"),
        ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료"),
    )
    fun verifyCode(@Valid @RequestBody request: UserEmailVerificationVerifyRequest) {
        emailVerificationService.verifyCode(request.email, request.code)
    }

    @PostMapping("/password-reset/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "비밀번호 재설정 코드 발송", description = "가입된 이메일로 재설정 코드를 발송 (5분 유효)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "발송 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "404", description = "가입되지 않은 이메일"),
    )
    fun sendUserPasswordResetCode(@Valid @RequestBody request: UserPasswordResetSendRequest) {
        passwordResetService.sendCode(request.email)
    }

    @PostMapping("/password-reset/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "비밀번호 재설정 코드 검증", description = "발송된 재설정 코드를 검증")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "검증 성공"),
        ApiResponse(responseCode = "400", description = "코드 불일치 또는 만료"),
    )
    fun verifyUserPasswordResetCode(@Valid @RequestBody request: UserPasswordResetVerifyRequest) {
        passwordResetService.verifyCode(request.email, request.code)
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "비밀번호 재설정", description = "코드 재검증 후 새 비밀번호로 변경")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "변경 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패 또는 코드 불일치/만료"),
        ApiResponse(responseCode = "404", description = "가입되지 않은 이메일"),
    )
    fun confirmUserPasswordReset(@Valid @RequestBody request: UserPasswordResetConfirmRequest) {
        passwordResetService.confirm(request.email, request.code, request.newPassword)
    }
}
