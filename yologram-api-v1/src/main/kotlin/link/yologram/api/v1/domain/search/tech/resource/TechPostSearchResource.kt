package link.yologram.api.v1.domain.search.tech.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import link.yologram.api.v1.domain.pms.tech.model.TechPostSummaryResponse
import link.yologram.api.v1.domain.search.tech.model.TechSearchSort
import link.yologram.api.v1.domain.search.tech.service.TechPostSearchService
import link.yologram.api.v1.domain.ums.resolver.OptionalAuthenticatedUser
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 테크 게시글 검색 API — 공개 다건 탐색이라 search 도메인이 담당한다
 * (단건·쓰기·"내 것"은 pms. docs/todos.md의 pms vs search 호출 기준).
 *
 * 페이징은 offset(page/size)이다 — 프론트가 페이지 네비게이션을 쓰고, 총건수·페이지 수가 필요하다.
 * 개인화(likedByMe)를 위해 선택 인증을 받는다: 헤더가 없으면 비로그인으로 처리하고 false를 준다.
 */
@Tag(name = "TechPostSearch", description = "테크 게시글 검색 (OpenSearch)")
@RestController
@RequestMapping("/api/v1/search/tech/posts")
class TechPostSearchResource(
    private val techPostSearchService: TechPostSearchService,
) {

    @GetMapping
    @Operation(
        summary = "게시글 검색",
        description = "제목·본문을 형태소(nori) 기준으로 검색한다. 제목 가중치 2배. 로그인 시 likedByMe 포함",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검색 성공"),
        ApiResponse(responseCode = "400", description = "검색어 없음(BLANK_SEARCH_KEYWORD) 또는 조회 한계 초과(SEARCH_PAGE_TOO_DEEP)"),
        ApiResponse(responseCode = "401", description = "인증 헤더가 있으나 토큰이 유효하지 않음"),
        ApiResponse(responseCode = "503", description = "검색 설정 없음 (SEARCH_UNAVAILABLE)"),
    )
    fun search(
        @Parameter(description = "검색어", example = "제미나이")
        @RequestParam q: String,

        @Parameter(description = "페이지 번호 (0부터)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "페이지 크기 (최대 50)", example = "10")
        @RequestParam(defaultValue = "10") size: Int,

        @Parameter(description = "정렬 기준")
        @RequestParam(defaultValue = "RELEVANCE") sort: TechSearchSort,

        @OptionalAuthenticatedUser authData: AuthData?,
    ): ApiEnvelopPage<TechPostSummaryResponse> {
        return techPostSearchService.search(
            keyword = q,
            page = page,
            size = size,
            sort = sort,
            viewerUid = authData?.uid,
        )
    }
}
