package link.yologram.api.v1.domain.ums.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import link.yologram.api.v1.domain.ums.model.AdminLoginRequest
import link.yologram.api.v1.domain.ums.model.AdminLoginResponse
import link.yologram.api.v1.domain.ums.model.AdminUserCreateRequest
import link.yologram.api.v1.domain.ums.model.AdminUserCreateResponse
import link.yologram.api.v1.domain.ums.model.AdminUserResponse
import link.yologram.api.v1.domain.ums.model.AdminValidateTokenResponse
import link.yologram.api.v1.domain.ums.resolver.AdminAuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUser
import link.yologram.api.v1.domain.ums.service.AdminUserService
import link.yologram.api.v1.global.model.ApiEnvelop
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "AdminUser", description = "어드민 유저 관리")
@RestController
@RequestMapping("/api/v1/ums/admin")
class AdminUserResource(
    private val adminUserService: AdminUserService,
) {

    @PostMapping("/admin-users")
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

    @GetMapping("/admin-users")
    @Operation(summary = "어드민 유저 목록 조회", description = "어드민을 id 오름차순 offset 페이지네이션으로 조회 (어드민 토큰 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공 (data + page/size/totalPages/totalCount/first/last)"),
        ApiResponse(responseCode = "400", description = "page/size 검증 실패 (page 0 이상, size 1~100)"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
    )
    fun getAdminUsers(
        @AuthenticatedAdminUser authData: AdminAuthData,
        @Parameter(description = "페이지 번호 (0부터 시작, 기본 0)")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지 크기 (기본 10, 1~100)")
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) size: Int,
    ): ApiEnvelopPage<AdminUserResponse> {
        return adminUserService.getAdminUsers(page, size)
    }

    @DeleteMapping("/admin-users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "어드민 유저 삭제", description = "어드민 계정 삭제 (hard delete, 어드민 토큰 필요). 자기 자신은 삭제 불가")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "삭제 성공"),
        ApiResponse(responseCode = "400", description = "자기 자신 삭제 시도 (ADMIN_USER_SELF_DELETE)"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
        ApiResponse(responseCode = "404", description = "어드민 사용자를 찾을 수 없음 (ADMIN_USER_NOT_FOUND)"),
    )
    fun deleteAdminUser(
        @AuthenticatedAdminUser authData: AdminAuthData,
        @PathVariable id: Long,
    ) {
        adminUserService.delete(authData.uid, id)
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

    @PostMapping("/auth/validate-token")
    @Operation(summary = "어드민 토큰 검증")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검증 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
        ApiResponse(responseCode = "404", description = "어드민 사용자를 찾을 수 없음"),
    )
    fun validateToken(@AuthenticatedAdminUser authData: AdminAuthData): ApiEnvelop<AdminValidateTokenResponse> {
        return ApiEnvelop(data = adminUserService.validateToken(authData.accessToken))
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "어드민 로그아웃")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "로그아웃 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
    )
    fun logout(@AuthenticatedAdminUser authData: AdminAuthData) {
        adminUserService.logout(authData.uid)
    }
}
