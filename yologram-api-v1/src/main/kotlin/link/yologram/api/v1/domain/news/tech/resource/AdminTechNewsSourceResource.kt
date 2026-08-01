package link.yologram.api.v1.domain.news.tech.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceCreateRequest
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceResponse
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceUpdateRequest
import link.yologram.api.v1.domain.news.tech.service.AdminTechNewsSourceService
import link.yologram.api.v1.domain.ums.resolver.AdminAuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUser
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 어드민 테크 뉴스 소스 관리 API — worker가 수집하는 RSS 피드 소스(tech_news_source) CRUD.
 * 경로는 도메인 뒤 admin 세그먼트 규칙(/news/admin/...) 적용.
 */
@Tag(name = "AdminTechNewsSource", description = "어드민 테크 뉴스 소스 관리 (RSS 피드)")
@RestController
@RequestMapping("/api/v1/news/admin/tech/sources")
class AdminTechNewsSourceResource(
    private val adminTechNewsSourceService: AdminTechNewsSourceService,
) {

    @GetMapping
    @Operation(summary = "테크 뉴스 소스 목록 조회", description = "전체 소스를 id 오름차순으로 조회 (어드민 토큰 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
    )
    fun getSources(
        @AuthenticatedAdminUser authData: AdminAuthData,
    ): ApiEnvelop<List<AdminTechNewsSourceResponse>> {
        return ApiEnvelop(data = adminTechNewsSourceService.getSources())
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "테크 뉴스 소스 생성", description = "RSS 피드 소스를 추가 (어드민 토큰 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "생성 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
        ApiResponse(responseCode = "409", description = "이미 등록된 뉴스 소스 URL (NEWS_SOURCE_DUPLICATE)"),
    )
    fun create(
        @AuthenticatedAdminUser authData: AdminAuthData,
        @Valid @RequestBody request: AdminTechNewsSourceCreateRequest,
    ): ApiEnvelop<AdminTechNewsSourceResponse> {
        return ApiEnvelop(data = adminTechNewsSourceService.create(request))
    }

    @PatchMapping("/{id}")
    @Operation(summary = "테크 뉴스 소스 수정", description = "널 필드는 미변경 (부분 갱신, 어드민 토큰 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
        ApiResponse(responseCode = "404", description = "뉴스 소스를 찾을 수 없음 (NEWS_SOURCE_NOT_FOUND)"),
        ApiResponse(responseCode = "409", description = "이미 등록된 뉴스 소스 URL (NEWS_SOURCE_DUPLICATE)"),
    )
    fun update(
        @AuthenticatedAdminUser authData: AdminAuthData,
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminTechNewsSourceUpdateRequest,
    ): ApiEnvelop<AdminTechNewsSourceResponse> {
        return ApiEnvelop(data = adminTechNewsSourceService.update(id, request))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "테크 뉴스 소스 삭제", description = "hard delete — 수집 중지는 isActive=false 사용 권장 (어드민 토큰 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "삭제 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
        ApiResponse(responseCode = "404", description = "뉴스 소스를 찾을 수 없음 (NEWS_SOURCE_NOT_FOUND)"),
    )
    fun delete(
        @AuthenticatedAdminUser authData: AdminAuthData,
        @PathVariable id: Long,
    ) {
        adminTechNewsSourceService.delete(id)
    }
}
