package link.yologram.api.v1.domain.news.tech.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "어드민 테크 뉴스 소스 생성 요청")
data class AdminTechNewsSourceCreateRequest(
    @field:NotBlank(message = "소스 이름을 입력해주세요")
    @field:Size(min = 1, max = 100, message = "소스 이름은 1~100자여야 합니다")
    @Schema(description = "소스 이름", example = "GeekNews")
    val name: String,

    @field:NotBlank(message = "RSS 피드 URL을 입력해주세요")
    @field:Size(max = 500, message = "URL은 500자 이하여야 합니다")
    @field:Pattern(regexp = "^https?://\\S+$", message = "URL은 http/https 형식이어야 합니다")
    @Schema(description = "RSS 피드 URL", example = "https://news.hada.io/rss/news")
    val url: String,

    @Schema(description = "수집 활성 여부 (생략 시 true)", example = "true")
    val isActive: Boolean = true,
)
