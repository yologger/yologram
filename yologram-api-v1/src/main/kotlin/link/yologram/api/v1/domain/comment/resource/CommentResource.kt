package link.yologram.api.v1.domain.comment.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.comment.model.CommentResponse
import link.yologram.api.v1.domain.comment.model.CreateCommentRequest
import link.yologram.api.v1.domain.comment.model.CreateCommentResponse
import link.yologram.api.v1.domain.comment.service.CommentService
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUser
import link.yologram.api.v1.global.model.ApiEnvelop
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Comment", description = "게시글 댓글")
@RestController
@RequestMapping("/api/v1/comments")
class CommentResource(
    private val commentService: CommentService,
) {

    @PostMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "댓글 작성", description = "게시글(postId)에 댓글을 작성 (인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "작성 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "404", description = "대상 게시글을 찾을 수 없음"),
    )
    fun create(
        @PathVariable postId: Long,
        @AuthenticatedUser authData: AuthData,
        @Valid @RequestBody request: CreateCommentRequest,
    ): ApiEnvelop<CreateCommentResponse> {
        return ApiEnvelop(data = commentService.create(postId, authData.uid, request))
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "댓글 목록 조회", description = "게시글(postId)의 댓글. 최신순(기본)/오래된순 cursor 페이지네이션 (공개)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "유효하지 않은 커서"),
    )
    fun getCommentsByCursor(
        @PathVariable postId: Long,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ApiEnvelopCursorPage<CommentResponse> {
        return commentService.getCommentsByCursor(postId, sort, cursor, size)
    }

    // 댓글 목록 (offset 페이지네이션) — 학습용. 코드는 CommentService.getCommentsByOffset에 보존, 엔드포인트는 비활성(필요 시 주석 해제)
//    @GetMapping("/posts/{postId}")
//    @Operation(summary = "댓글 목록 조회 (offset, 학습용)", description = "게시글(postId)의 댓글. offset 페이지네이션 + 전체 count (공개). cursor 방식(/posts/{postId})과 대비되는 학습용")
//    @ApiResponses(
//        ApiResponse(responseCode = "200", description = "조회 성공"),
//    )
//    fun getCommentsByOffset(
//        @PathVariable postId: Long,
//        @RequestParam(required = false) sort: String?,
//        @RequestParam(required = false, defaultValue = "0") page: Int,
//        @RequestParam(required = false, defaultValue = "20") size: Int,
//    ): ApiEnvelopPage<CommentResponse> {
//        return commentService.getCommentsByOffset(postId, sort, page, size)
//    }
}
