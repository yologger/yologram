package link.yologram.api.v1.domain.search.tech.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import link.yologram.api.v1.domain.search.tech.service.AdminTechPostIndexingService
import link.yologram.api.v1.domain.ums.resolver.AdminAuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUser
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 어드민 게시글 인덱싱 API (레거시 BoardIndexingResource 미러) — 검색 인덱스 재구축 조작.
 * 경로는 도메인 뒤 admin 세그먼트 규칙(/search/admin/...) 적용.
 *
 * posts 뒤에 indexing을 붙인 이유: 이 API는 게시글을 수정하지 않고 색인 작업만 큐에 넣는다.
 * /posts에 PUT을 걸면 게시글 수정으로 읽히고, 검색 API(/search/tech/posts)와도 성격이 뒤섞인다.
 * indexing을 조작 세그먼트로 두면 인덱스 관리(/posts/indices)·재색인 전환(/posts/migration)이
 * 형제로 붙을 자리가 남는다.
 *
 * 세 엔드포인트 모두 SQS에 작업을 넣고 즉시 202로 응답한다 — 실제 인덱싱은 worker가 비동기로 수행한다.
 * 레거시는 단건만 동기 처리(200)했지만 우리는 경로를 하나로 합쳤다(from == to).
 *
 * 인덱싱은 운영 조작이라 어드민 토큰을 요구한다. 공개 API로 열면 누구나 풀 인덱싱을 유발해
 * OpenSearch와 DB에 부하를 줄 수 있다.
 */
@Tag(name = "AdminTechPostIndexing", description = "어드민 테크 게시글 검색 인덱싱 (SQS 작업 발행)")
@RestController
@RequestMapping("/api/v1/search/admin/tech/posts/indexing")
class AdminTechPostIndexingResource(
    private val adminTechPostIndexingService: AdminTechPostIndexingService,
) {

    @PutMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "게시글 전체 인덱싱",
        description = "1 ~ max(id) 범위를 20건 단위로 쪼개 SQS에 발행한다. 발행 자체가 백그라운드라 즉시 202로 응답하고, 진행 상황은 SQS 큐 깊이로 확인한다. 실제 인덱싱은 worker가 수행 (어드민 토큰 필요)",
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "인덱싱 작업 발행 완료"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
    )
    fun fullIndex(
        @AuthenticatedAdminUser authData: AdminAuthData,
    ) {
        adminTechPostIndexingService.fullIndexAsync()
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "게시글 단건 인덱싱",
        description = "해당 id 하나만 인덱싱 (from == to로 범위 인덱싱과 같은 경로) (어드민 토큰 필요)",
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "인덱싱 작업 발행 완료"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
    )
    fun index(
        @AuthenticatedAdminUser authData: AdminAuthData,
        @PathVariable id: Long,
    ) {
        adminTechPostIndexingService.index(id)
    }

    @PutMapping("/{from}/{to}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "게시글 범위 인덱싱",
        description = "from ~ to 범위를 20건 단위로 쪼개 SQS에 발행 (어드민 토큰 필요)",
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "인덱싱 작업 발행 완료"),
        ApiResponse(responseCode = "400", description = "from > to"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
    )
    fun index(
        @AuthenticatedAdminUser authData: AdminAuthData,
        @PathVariable from: Long,
        @PathVariable to: Long,
    ) {
        adminTechPostIndexingService.index(from = from, to = to)
    }
}
