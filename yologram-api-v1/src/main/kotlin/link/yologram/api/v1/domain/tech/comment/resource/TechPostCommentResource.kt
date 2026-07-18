package link.yologram.api.v1.domain.tech.comment.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.tech.comment.model.CreateTechPostCommentRequest
import link.yologram.api.v1.domain.tech.comment.model.CreateTechPostCommentResponse
import link.yologram.api.v1.domain.tech.comment.model.TechPostCommentResponse
import link.yologram.api.v1.domain.tech.comment.model.UpdateTechPostCommentRequest
import link.yologram.api.v1.domain.tech.comment.service.TechPostCommentService
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

/** 테크 게시글 댓글 API (신규 경로: /comments/tech/...). 구경로 호환은 LegacyCommentResource 참조. */
@Tag(name = "TechPostComment", description = "테크 게시글 댓글")
@RestController
@RequestMapping("/api/v1/comments")
class TechPostCommentResource(
    private val commentService: TechPostCommentService,
) {

    @PostMapping("/tech/posts/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "테크 댓글 작성", description = "테크 게시글(postId)에 댓글을 작성 (인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "작성 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "404", description = "대상 게시글을 찾을 수 없음"),
    )
    fun create(
        @PathVariable postId: Long,
        @AuthenticatedUser authData: AuthData,
        @Valid @RequestBody request: CreateTechPostCommentRequest,
    ): ApiEnvelop<CreateTechPostCommentResponse> {
        return ApiEnvelop(data = commentService.create(postId, authData.uid, request))
    }

    @PatchMapping("/tech/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "테크 댓글 수정", description = "본인 댓글 수정 (인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "수정 성공"),
        ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "403", description = "본인 댓글이 아님"),
        ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
    )
    fun update(
        @PathVariable commentId: Long,
        @AuthenticatedUser authData: AuthData,
        @Valid @RequestBody request: UpdateTechPostCommentRequest,
    ) {
        commentService.update(commentId, authData.uid, request)
    }

    @DeleteMapping("/tech/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "테크 댓글 삭제", description = "본인 댓글 삭제 (인증 필요)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "삭제 성공"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "403", description = "본인 댓글이 아님"),
        ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
    )
    fun delete(
        @PathVariable commentId: Long,
        @AuthenticatedUser authData: AuthData,
    ) {
        commentService.delete(commentId, authData.uid)
    }

    @GetMapping("/tech/posts/{postId}")
    @Operation(summary = "테크 댓글 목록 조회", description = "테크 게시글(postId)의 댓글. 최신순(기본)/오래된순 cursor 페이지네이션 (공개)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "유효하지 않은 커서"),
    )
    fun getCommentsByCursor(
        @PathVariable postId: Long,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ApiEnvelopCursorPage<TechPostCommentResponse> {
        return commentService.getCommentsByCursor(postId, sort, cursor, size)
    }

    // 댓글 목록 (offset 페이지네이션) — 학습용. 코드는 TechPostCommentService.getCommentsByOffset에 보존, 엔드포인트는 비활성(필요 시 주석 해제)
//    @GetMapping("/tech/posts/{postId}/offset")
//    @Operation(summary = "테크 댓글 목록 조회 (offset, 학습용)", description = "테크 게시글(postId)의 댓글. offset 페이지네이션 + 전체 count (공개). cursor 방식(/tech/posts/{postId})과 대비되는 학습용")
//    @ApiResponses(
//        ApiResponse(responseCode = "200", description = "조회 성공"),
//    )
//    fun getCommentsByOffset(
//        @PathVariable postId: Long,
//        @RequestParam(required = false) sort: String?,
//        @RequestParam(required = false, defaultValue = "0") page: Int,
//        @RequestParam(required = false, defaultValue = "20") size: Int,
//    ): ApiEnvelopPage<TechPostCommentResponse> {
//        return commentService.getCommentsByOffset(postId, sort, page, size)
//    }
}
