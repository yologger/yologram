package link.yologram.api.v1.domain.search.tech.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import link.yologram.api.v1.domain.news.tech.model.TechNewsResponse
import link.yologram.api.v1.domain.search.tech.model.TechSearchSort
import link.yologram.api.v1.domain.search.tech.service.TechNewsSearchService
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 테크 뉴스 검색 API — 공개 다건 탐색이라 search 도메인이 담당한다
 * (목록·단건은 news 도메인. docs/todos.md의 호출 기준).
 *
 * 페이징은 offset(page/size)이다 — 프론트가 페이지 네비게이션을 쓰고, 총건수·페이지 수가 필요하다
 * (뉴스 목록 API는 무한스크롤이라 커서를 쓰지만 검색은 다르다).
 *
 * 게시글 검색과 달리 인증을 받지 않는다 — 뉴스에는 개인화 값(likedByMe 같은)이 없다.
 */
@Tag(name = "TechNewsSearch", description = "테크 뉴스 검색 (OpenSearch)")
@RestController
@RequestMapping("/api/v1/search/tech/news")
class TechNewsSearchResource(
    private val techNewsSearchService: TechNewsSearchService,
) {

    @GetMapping
    @Operation(
        summary = "뉴스 검색",
        description = "제목·요약을 형태소(nori) 기준으로 검색한다. 제목 가중치 2배. 최신순은 발행 시각(publishedAt) 기준",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검색 성공"),
        ApiResponse(responseCode = "400", description = "검색어 없음(BLANK_SEARCH_KEYWORD) 또는 조회 한계 초과(SEARCH_PAGE_TOO_DEEP)"),
        ApiResponse(responseCode = "503", description = "검색 설정 없음 (SEARCH_UNAVAILABLE)"),
    )
    fun search(
        @Parameter(description = "검색어", example = "마이그레이션")
        @RequestParam q: String,

        @Parameter(description = "페이지 번호 (0부터)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "페이지 크기 (최대 50)", example = "10")
        @RequestParam(defaultValue = "10") size: Int,

        @Parameter(description = "정렬 기준")
        @RequestParam(defaultValue = "RELEVANCE") sort: TechSearchSort,
    ): ApiEnvelopPage<TechNewsResponse> {
        return techNewsSearchService.search(keyword = q, page = page, size = size, sort = sort)
    }
}
