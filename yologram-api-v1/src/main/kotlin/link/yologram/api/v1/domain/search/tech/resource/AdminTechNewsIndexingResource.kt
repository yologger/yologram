package link.yologram.api.v1.domain.search.tech.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import link.yologram.api.v1.domain.search.tech.service.AdminTechNewsIndexingService
import link.yologram.api.v1.domain.ums.resolver.AdminAuthData
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUser
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 어드민 뉴스 인덱싱 API — 게시글 인덱싱(AdminTechPostIndexingResource)과 같은 구조·같은 큐를 쓴다.
 *
 * 게시글과 달리 평상시 색인은 이 API가 아니라 worker의 요약 배치가 직접 한다(요약 완료 직후 색인).
 * 여기는 그 실시간 경로가 놓친 구간을 메우는 보정 도구다 — 색인 실패로 빠진 건,
 * 매핑 변경 후 재색인, 검색을 나중에 켠 경우의 과거 데이터.
 *
 * 세 엔드포인트 모두 SQS에 작업을 넣고 즉시 202로 응답한다 — 실제 인덱싱은 worker가 비동기로 수행한다.
 */
@Tag(name = "AdminTechNewsIndexing", description = "어드민 테크 뉴스 검색 인덱싱 (SQS 작업 발행)")
@RestController
@RequestMapping("/api/v1/search/admin/tech/news/indexing")
class AdminTechNewsIndexingResource(
    private val adminTechNewsIndexingService: AdminTechNewsIndexingService,
) {

    @PutMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "뉴스 전체 인덱싱",
        description = "1 ~ max(id) 범위를 20건 단위로 쪼개 SQS에 발행한다. 발행 자체가 백그라운드라 즉시 202로 응답하고, 진행 상황은 SQS 큐 깊이로 확인한다. 실제 인덱싱은 worker가 수행 (어드민 토큰 필요)",
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "인덱싱 작업 발행 완료"),
        ApiResponse(responseCode = "401", description = "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"),
    )
    fun fullIndex(
        @AuthenticatedAdminUser authData: AdminAuthData,
    ) {
        adminTechNewsIndexingService.fullIndexAsync()
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "뉴스 단건 인덱싱",
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
        adminTechNewsIndexingService.index(id)
    }

    @PutMapping("/{from}/{to}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "뉴스 범위 인덱싱",
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
        adminTechNewsIndexingService.index(from = from, to = to)
    }
}
