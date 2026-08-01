package link.yologram.api.v1.domain.pms.tech.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.pms.tech.model.CreateTechPostRequest
import link.yologram.api.v1.domain.pms.tech.model.CreateTechPostResponse
import link.yologram.api.v1.domain.pms.tech.model.TechPostDetailResponse
import link.yologram.api.v1.domain.pms.tech.model.TechPostSummaryResponse
import link.yologram.api.v1.domain.pms.tech.model.UpdateTechPostRequest
import link.yologram.api.v1.domain.pms.tech.service.TechPostService
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUser
import link.yologram.api.v1.global.model.ApiEnvelop
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 테크 게시판 게시글 API.
 * 구 /pms/{section}/posts의 section 경로변수를 tech 고정 매핑으로 전환 — URL 결과는 동일.
 */
@Tag(name = "TechPost", description = "테크 게시판 게시글")
@RestController
@RequestMapping("/api/v1/pms")
class TechPostResource(
    private val postService: TechPostService,
) {

    @PostMapping("/tech/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "테크 게시글 작성", description = "테크 게시판에 게시글을 작성 (인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "작성 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패 / 카테고리 불일치"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
    )
    fun create(
        @AuthenticatedUser authData: AuthData,
        @Valid @RequestBody request: CreateTechPostRequest,
    ): ApiEnvelop<CreateTechPostResponse> {
        return ApiEnvelop(data = postService.create(authData.uid, request))
    }

    @PatchMapping("/tech/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "테크 게시글 수정", description = "본인 게시글 수정 (인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패 / 카테고리 불일치"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "403", description = "본인 글이 아님"),
        ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
    )
    fun update(
        @PathVariable id: Long,
        @AuthenticatedUser authData: AuthData,
        @Valid @RequestBody request: UpdateTechPostRequest,
    ) {
        postService.update(id, authData.uid, request)
    }

    @DeleteMapping("/tech/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "테크 게시글 삭제", description = "본인 게시글 삭제 (인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "삭제 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "403", description = "본인 글이 아님"),
        ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
    )
    fun delete(
        @PathVariable id: Long,
        @AuthenticatedUser authData: AuthData,
    ) {
        postService.delete(id, authData.uid)
    }

    // 게시글 피드 (cursor-based pagination)
    @GetMapping("/tech/posts")
    @Operation(summary = "테크 게시글 목록 조회", description = "테크 피드. 최신순 cursor 페이지네이션 (공개)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "유효하지 않은 커서"),
    )
    fun getPosts(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) categoryId: Long?,
    ): ApiEnvelopCursorPage<TechPostSummaryResponse> {
        return postService.getPostsByCursor(categoryId, cursor, size)
    }

    // 게시글 피드 (offset 페이지네이션) — 학습용. 코드는 TechPostService.getPostsByOffset에 보존, 엔드포인트는 비활성(필요 시 주석 해제)
//    @GetMapping("/tech/posts/offset")
//    @Operation(summary = "테크 게시글 목록 조회 (offset, 학습용)", description = "테크 피드. offset 페이지네이션 + 전체 count (공개). cursor 방식(/tech/posts)과 대비되는 학습용")
//    @ApiResponses(
//        ApiResponse(responseCode = "200", description = "조회 성공"),
//    )
//    fun getPostsByOffset(
//        @RequestParam(required = false) categoryId: Long?,
//        @RequestParam(required = false, defaultValue = "0") page: Int,
//        @RequestParam(required = false, defaultValue = "20") size: Int,
//    ): ApiEnvelopPage<TechPostSummaryResponse> {
//        return postService.getPostsByOffset(categoryId, page, size)
//    }

    @GetMapping("/posts/me")
    @Operation(summary = "내 글 목록 조회", description = "로그인 유저가 작성한 글. 최신순 cursor 페이지네이션 (인증 필요). section은 구 API 호환 파라미터(tech만 허용, 생략 가능)")
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
    ): ApiEnvelopCursorPage<TechPostSummaryResponse> {
        return postService.getMyPostsByCursor(authData.uid, section, cursor, size)
    }

    // 내 글 목록 (offset 페이지네이션) — 학습용. 코드는 TechPostService.getMyPostsByOffset에 보존, 엔드포인트는 비활성(필요 시 주석 해제)
//    @GetMapping("/posts/me/offset")
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
//    ): ApiEnvelopPage<TechPostSummaryResponse> {
//        return postService.getMyPostsByOffset(authData.uid, section, page, size)
//    }

    @GetMapping("/tech/posts/{id}")
    @Operation(summary = "테크 게시글 상세 조회", description = "테크 게시판의 게시글 단건 조회 (공개)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
    )
    fun getPost(
        @PathVariable id: Long,
    ): ApiEnvelop<TechPostDetailResponse> {
        return ApiEnvelop(data = postService.getPost(id))
    }
}
