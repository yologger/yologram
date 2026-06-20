package link.yologram.api.v1.domain.cms.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import link.yologram.api.v1.domain.cms.model.PostCategoryResponse
import link.yologram.api.v1.domain.cms.service.PostCategoryService
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "PostCategory", description = "커뮤니티 카테고리 (contents)")
@RestController
@RequestMapping("/api/v1/cms")
class PostCategoryResource(
    private val categoryService: PostCategoryService,
) {

    @GetMapping("/{section}/categories")
    @Operation(summary = "카테고리 목록 조회", description = "섹션(section)별 활성 카테고리를 정렬 순으로 조회")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "유효하지 않은 섹션"),
    )
    fun getPostCategories(@PathVariable section: String): ApiEnvelop<List<PostCategoryResponse>> {
        return ApiEnvelop(data = categoryService.getPostCategories(section))
    }
}
