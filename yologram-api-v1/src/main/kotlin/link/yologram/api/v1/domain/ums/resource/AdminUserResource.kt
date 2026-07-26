package link.yologram.api.v1.domain.ums.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.ums.model.AdminLoginRequest
import link.yologram.api.v1.domain.ums.model.AdminLoginResponse
import link.yologram.api.v1.domain.ums.model.AdminUserCreateRequest
import link.yologram.api.v1.domain.ums.model.AdminUserCreateResponse
import link.yologram.api.v1.domain.ums.resolver.AdminAuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUser
import link.yologram.api.v1.domain.ums.service.AdminUserService
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "AdminUser", description = "어드민 유저 관리")
@RestController
@RequestMapping("/api/v1/ums/admin")
class AdminUserResource(
    private val adminUserService: AdminUserService,
) {

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "어드민 유저 생성", description = "기존 어드민이 새 어드민 계정을 추가 (어드민 토큰 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
        ApiResponse(responseCode = "409", description = "이미 등록된 어드민 이메일"),
    )
    fun create(
        @AuthenticatedAdminUser authData: AdminAuthData,
        @Valid @RequestBody request: AdminUserCreateRequest,
    ): ApiEnvelop<AdminUserCreateResponse> {
        return ApiEnvelop(data = adminUserService.create(request))
    }

    @PostMapping("/auth/login")
    @Operation(summary = "어드민 로그인")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "비밀번호 불일치"),
        ApiResponse(responseCode = "404", description = "어드민 사용자를 찾을 수 없음"),
    )
    fun login(@Valid @RequestBody request: AdminLoginRequest): ApiEnvelop<AdminLoginResponse> {
        return ApiEnvelop(data = adminUserService.login(request))
    }
}
