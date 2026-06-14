package link.yologram.api.v1.domain.ums.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.ums.model.ChangePasswordRequest
import link.yologram.api.v1.domain.ums.model.JoinRequest
import link.yologram.api.v1.domain.ums.model.JoinResponse
import link.yologram.api.v1.domain.ums.model.UpdateProfileRequest
import link.yologram.api.v1.domain.ums.model.UserMeResponse
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUser
import link.yologram.api.v1.domain.ums.service.UserService
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "User", description = "유저 관리")
@RestController
@RequestMapping("/api/v1/ums/user")
class UserResource(
    private val userService: UserService,
) {

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회원가입")
    fun join(@Valid @RequestBody request: JoinRequest): ApiEnvelop<JoinResponse> {
        return ApiEnvelop(data = userService.join(request))
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "인증된 사용자의 프로필 정보를 조회")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패 (토큰 없음/만료/유효하지 않음)"),
        ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
    )
    fun getMe(@AuthenticatedUser authData: AuthData): ApiEnvelop<UserMeResponse> {
        return ApiEnvelop(data = userService.getMe(authData.uid))
    }

    @PatchMapping("/me")
    @Operation(summary = "회원정보 수정", description = "닉네임 변경")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
    )
    fun updateProfile(
        @AuthenticatedUser authData: AuthData,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): ApiEnvelop<UserMeResponse> {
        return ApiEnvelop(data = userService.updateProfile(authData.uid, request))
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "변경 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패 또는 현재 비밀번호 불일치"),
        ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
    )
    fun changePassword(
        @AuthenticatedUser authData: AuthData,
        @Valid @RequestBody request: ChangePasswordRequest,
    ) {
        userService.changePassword(authData.uid, request)
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "회원탈퇴", description = "본인 계정을 탈퇴 처리 (soft delete: status=DELETED)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "탈퇴 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
    )
    fun withdraw(@AuthenticatedUser authData: AuthData) {
        userService.withdraw(authData.uid)
    }
}
