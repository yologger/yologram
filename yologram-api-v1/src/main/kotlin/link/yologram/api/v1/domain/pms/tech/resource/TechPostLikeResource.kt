package link.yologram.api.v1.domain.pms.tech.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import link.yologram.api.v1.domain.pms.tech.service.TechPostLikeService
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUser
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 테크 게시글 좋아요 API. 멱등이라 성공은 항상 200 —
 * 중복 좋아요/미좋아요 취소도 에러 없이 현재 상태로 수렴 (더블클릭·네트워크 재시도 안전).
 */
@Tag(name = "TechPostLike", description = "테크 게시판 게시글 좋아요")
@RestController
@RequestMapping("/api/v1/pms")
class TechPostLikeResource(
    private val likeService: TechPostLikeService,
) {

    @PostMapping("/tech/posts/{id}/like")
    @Operation(summary = "테크 게시글 좋아요", description = "게시글에 좋아요 (인증 필요). 이미 눌렀으면 no-op 200 (멱등)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좋아요 성공 (이미 누른 상태 포함)"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
    )
    fun like(
        @PathVariable id: Long,
        @AuthenticatedUser authData: AuthData,
    ) {
        likeService.like(id, authData.uid)
    }

    @DeleteMapping("/tech/posts/{id}/like")
    @Operation(summary = "테크 게시글 좋아요 취소", description = "게시글 좋아요 취소 (인증 필요). 안 누른 상태면 no-op 200 (멱등)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "취소 성공 (안 누른 상태 포함)"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
    )
    fun unlike(
        @PathVariable id: Long,
        @AuthenticatedUser authData: AuthData,
    ) {
        likeService.unlike(id, authData.uid)
    }
}
