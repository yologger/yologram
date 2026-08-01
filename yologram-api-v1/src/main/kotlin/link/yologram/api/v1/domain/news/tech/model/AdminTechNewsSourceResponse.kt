package link.yologram.api.v1.domain.news.tech.model

import io.swagger.v3.oas.annotations.media.Schema
import link.yologram.api.v1.domain.news.tech.entity.TechNewsSource
import java.time.LocalDateTime

@Schema(description = "어드민 테크 뉴스 소스")
data class AdminTechNewsSourceResponse(
    val id: Long,
    @Schema(description = "소스 이름", example = "GeekNews")
    val name: String,
    @Schema(description = "RSS 피드 URL", example = "https://news.hada.io/rss/news")
    val url: String,
    @Schema(description = "수집 활성 여부", example = "true")
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val modifiedDate: LocalDateTime,
) {
    companion object {
        fun from(source: TechNewsSource) = AdminTechNewsSourceResponse(
            id = source.id,
            name = source.name,
            url = source.url,
            isActive = source.isActive,
            createdAt = source.createdAt,
            modifiedDate = source.modifiedDate,
        )
    }
}
