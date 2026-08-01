package link.yologram.api.v1.domain.news.tech.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.domain.news.tech.service.TechNewsService
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 테크 뉴스 공개 조회 API — worker가 수집·요약한 tech_news를 발행순으로 제공.
 * 섹션이 경로 세그먼트(/news/tech) — invest/politics 오픈 시 세그먼트 추가 (섹션 규약).
 */
@Tag(name = "TechNews", description = "테크 뉴스 (RSS 수집 + LLM 요약)")
@RestController
@RequestMapping("/api/v1/news")
class TechNewsResource(
    private val techNewsService: TechNewsService,
) {

    @GetMapping("/tech")
    @Operation(
        summary = "테크 뉴스 목록 조회",
        description = "요약 완료된 테크 뉴스를 발행순(published_at desc)으로 조회. (publishedAt, id) 복합 keyset cursor",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "유효하지 않은 커서 (INVALID_CURSOR)"),
    )
    fun getNews(
        @Parameter(description = "카테고리 id 필터 (tech_category — /cms/tech/categories 응답의 id. 생략 시 전체)")
        @RequestParam(required = false) categoryId: Long?,
        @Parameter(description = "이전 페이지 마지막 항목의 커서 (첫 페이지는 생략)")
        @RequestParam(required = false) cursor: String?,
        @Parameter(description = "페이지 크기 (기본 20, 최대 50)")
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiEnvelopCursorPage<TechNewsResponse> {
        return techNewsService.getNewsByCursor(categoryId, cursor, size)
    }
}
