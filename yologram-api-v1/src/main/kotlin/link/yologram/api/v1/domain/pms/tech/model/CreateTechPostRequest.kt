package link.yologram.api.v1.domain.pms.tech.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "테크 게시글 작성 요청")
data class CreateTechPostRequest(
    @field:Size(max = 200)
    @Schema(description = "제목 (선택)", example = "Next.js App Router 전환 후기", nullable = true)
    val title: String? = null,

    @field:NotBlank(message = "내용을 입력해주세요.")
    @Schema(description = "내용", example = "전환하면서 겪은 점 공유합니다")
    val content: String? = null,

    @field:Size(min = 1, max = 3, message = "카테고리는 1~3개 선택해주세요.")
    @Schema(description = "카테고리 ID 목록 (1~3개 필수)", example = "[1, 2]")
    val categoryIds: List<Long> = emptyList(),
)
