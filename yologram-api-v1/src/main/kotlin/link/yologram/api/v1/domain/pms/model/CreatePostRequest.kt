package link.yologram.api.v1.domain.pms.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "게시글 작성 요청")
data class CreatePostRequest(
    @field:Size(max = 200)
    @Schema(description = "제목 (선택)", example = "Next.js App Router 전환 후기", nullable = true)
    val title: String? = null,

    @field:NotBlank(message = "내용을 입력해주세요.")
    @Schema(description = "내용", example = "전환하면서 겪은 점 공유합니다")
    val content: String? = null,

    @field:Size(max = 3)
    @Schema(description = "카테고리 ID 목록 (최대 3개, 선택)", example = "[1, 2]")
    val categoryIds: List<Long> = emptyList(),
)
