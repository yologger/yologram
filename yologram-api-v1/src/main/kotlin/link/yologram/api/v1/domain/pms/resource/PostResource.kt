package link.yologram.api.v1.domain.pms.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import link.yologram.api.v1.domain.pms.model.CreatePostRequest
import link.yologram.api.v1.domain.pms.model.CreatePostResponse
import link.yologram.api.v1.domain.pms.service.PostService
import link.yologram.api.v1.domain.ums.resolver.AuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUser
import link.yologram.api.v1.global.model.ApiEnvelop
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
}
