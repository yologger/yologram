package link.yologram.api.v1.domain.news.tech.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/** 어드민 테크 뉴스 소스 수정 요청 — 널 필드는 미변경 (부분 갱신) */
@Schema(description = "어드민 테크 뉴스 소스 수정 요청 (널 필드는 미변경)")
data class AdminTechNewsSourceUpdateRequest(
    @field:Size(min = 1, max = 100, message = "소스 이름은 1~100자여야 합니다")
    @field:Pattern(regexp = "^(?s).*\\S.*$", message = "소스 이름은 공백일 수 없습니다")
    @Schema(description = "소스 이름 (미변경 시 생략)", example = "GeekNews")
    val name: String? = null,

    @field:Size(max = 500, message = "URL은 500자 이하여야 합니다")
    @field:Pattern(regexp = "^https?://\\S+$", message = "URL은 http/https 형식이어야 합니다")
    @Schema(description = "RSS 피드 URL (미변경 시 생략)", example = "https://news.hada.io/rss/news")
    val url: String? = null,

    @Schema(description = "수집 활성 여부 (미변경 시 생략)", example = "false")
    val isActive: Boolean? = null,
)
