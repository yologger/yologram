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

/**
 * 댓글 구경로 호환 (deprecated) — web이 신규 경로(/comments/tech/...)로 전환할 때까지 유지.
 * 모든 엔드포인트는 TechPostCommentService로 위임하며 응답은 신규 경로와 동일하다.
 * web 전환 완료 후 이 클래스만 삭제하면 된다.
 */
@Tag(name = "TechPostComment (Legacy)", description = "게시글 댓글 구경로 (deprecated — /comments/tech/... 사용 권장)")
@RestController
@RequestMapping("/api/v1/comments")
class LegacyCommentResource(
    private val commentService: TechPostCommentService,
) {

    @Deprecated("신규 경로 POST /api/v1/comments/tech/posts/{postId} 사용")
    @PostMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "댓글 작성 (구경로, deprecated)",
        description = "POST /api/v1/comments/tech/posts/{postId}로 대체됨. 테크 댓글 서비스로 위임 (인증 필요)",
        deprecated = true,
    )
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

    @Deprecated("신규 경로 PATCH /api/v1/comments/tech/{commentId} 사용")
    @PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "댓글 수정 (구경로, deprecated)",
        description = "PATCH /api/v1/comments/tech/{commentId}로 대체됨. 테크 댓글 서비스로 위임 (인증 필요)",
        deprecated = true,
    )
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

    @Deprecated("신규 경로 DELETE /api/v1/comments/tech/{commentId} 사용")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "댓글 삭제 (구경로, deprecated)",
        description = "DELETE /api/v1/comments/tech/{commentId}로 대체됨. 테크 댓글 서비스로 위임 (인증 필요)",
        deprecated = true,
    )
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

    @Deprecated("신규 경로 GET /api/v1/comments/tech/posts/{postId} 사용")
    @GetMapping("/posts/{postId}")
    @Operation(
        summary = "댓글 목록 조회 (구경로, deprecated)",
        description = "GET /api/v1/comments/tech/posts/{postId}로 대체됨. 테크 댓글 서비스로 위임 (공개)",
        deprecated = true,
    )
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
}
