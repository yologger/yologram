package link.yologram.api.v1.domain.tech.post.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "테크 게시글 수정 요청 (작성과 동일 검증)")
data class UpdateTechPostRequest(
    @field:Size(max = 200)
    @Schema(description = "제목 (선택)", example = "수정한 제목", nullable = true)
    val title: String? = null,

    @field:NotBlank(message = "내용을 입력해주세요.")
    @Schema(description = "내용", example = "수정한 내용")
    val content: String? = null,

    @field:Size(min = 1, max = 3, message = "카테고리는 1~3개 선택해주세요.")
    @Schema(description = "카테고리 ID 목록 (1~3개 필수)", example = "[1, 2]")
    val categoryIds: List<Long> = emptyList(),
)
