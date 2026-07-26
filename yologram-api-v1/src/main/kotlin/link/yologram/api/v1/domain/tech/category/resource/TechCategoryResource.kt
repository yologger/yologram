package link.yologram.api.v1.domain.tech.category.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import link.yologram.api.v1.domain.tech.category.model.TechCategoryResponse
import link.yologram.api.v1.domain.tech.category.service.TechCategoryService
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 테크 게시판 카테고리 API.
 * 구 /cms/{section}/categories의 section 경로변수를 tech 고정 매핑으로 전환 — URL 결과는 동일.
 */
@Tag(name = "TechCategory", description = "테크 카테고리 (게시판·뉴스 공용 마스터)")
@RestController
@RequestMapping("/api/v1/cms")
class TechCategoryResource(
    private val categoryService: TechCategoryService,
) {

    @GetMapping("/tech/categories")
    @Operation(summary = "테크 카테고리 목록 조회", description = "테크 게시판의 활성 카테고리를 정렬 순으로 조회")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
    )
    fun getCategories(): ApiEnvelop<List<TechCategoryResponse>> {
        return ApiEnvelop(data = categoryService.getCategories())
    }
}
