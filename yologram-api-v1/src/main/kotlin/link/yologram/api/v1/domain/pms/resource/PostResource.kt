package link.yologram.api.v1.domain.pms.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.pms.model.CreatePostRequest
import link.yologram.api.v1.domain.pms.model.CreatePostResponse
import link.yologram.api.v1.domain.pms.model.PostDetailResponse
import link.yologram.api.v1.domain.pms.model.PostSummaryResponse
import link.yologram.api.v1.domain.pms.service.PostService
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUser
import link.yologram.api.v1.global.model.ApiEnvelop
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Post", description = "커뮤니티 게시글")
@RestController
@RequestMapping("/api/v1/pms")
class PostResource(
    private val postService: PostService,
) {

    @PostMapping("/{section}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "게시글 작성", description = "섹션(section)에 게시글을 작성 (인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "작성 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패 / 유효하지 않은 섹션 / 카테고리 불일치"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
    )
    fun create(
        @PathVariable section: String,
        @AuthenticatedUser authData: AuthData,
        @Valid @RequestBody request: CreatePostRequest,
    ): ApiEnvelop<CreatePostResponse> {
        return ApiEnvelop(data = postService.create(section, authData.uid, request))
    }

    @GetMapping("/{section}/posts")
    @Operation(summary = "게시글 목록 조회", description = "섹션(section) 피드. 최신순 cursor 페이지네이션 (공개)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "유효하지 않은 섹션"),
    )
    fun getPosts(
        @PathVariable section: String,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) categoryId: Long?,
    ): ApiEnvelopCursorPage<PostSummaryResponse> {
        return postService.getPosts(section, categoryId, cursor, size)
    }

    @GetMapping("/posts/me")
    @Operation(summary = "내 글 목록 조회", description = "로그인 유저가 작성한 글. 최신순 cursor 페이지네이션 (인증 필요). section 생략 시 전체")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "유효하지 않은 섹션 / 커서"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
    )
    fun getMyPostsByCursor(
        @AuthenticatedUser authData: AuthData,
        @RequestParam(required = false) section: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ApiEnvelopCursorPage<PostSummaryResponse> {
        return postService.getMyPostsByCursor(authData.uid, section, cursor, size)
    }

//
//    @GetMapping("/posts/me")
//    @Operation(summary = "내 글 목록 조회 (offset, 학습용)", description = "로그인 유저가 작성한 글. 최신순 offset 페이지네이션 + 전체 count (인증 필요). cursor 방식(/posts/me)과 대비되는 학습용")
//    @ApiResponses(
//        ApiResponse(responseCode = "200", description = "조회 성공"),
//        ApiResponse(responseCode = "400", description = "유효하지 않은 섹션"),
//        ApiResponse(responseCode = "401", description = "인증 실패"),
//    )
//    fun getMyPostsByOffset(
//        @AuthenticatedUser authData: AuthData,
//        @RequestParam(required = false) section: String?,
//        @RequestParam(required = false, defaultValue = "0") page: Int,
//        @RequestParam(required = false, defaultValue = "20") size: Int,
//    ): ApiEnvelopPage<PostSummaryResponse> {
//        return postService.getMyPostsByOffset(authData.uid, section, page, size)
//    }

    @GetMapping("/{section}/posts/{id}")
    @Operation(summary = "게시글 상세 조회", description = "섹션(section)의 게시글 단건 조회 (공개)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "유효하지 않은 섹션"),
        ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
    )
    fun getPost(
        @PathVariable section: String,
        @PathVariable id: Long,
    ): ApiEnvelop<PostDetailResponse> {
        return ApiEnvelop(data = postService.getPost(section, id))
    }
}
