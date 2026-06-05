package link.yologram.api.v1.global.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonAutoDetect(fieldVisibility = ANY, isGetterVisibility = NONE)
data class ApiEnvelop<T>(
    @Schema(description = "data", type = "object", nullable = false)
    val data: T
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiEnvelopPage<T>(
    val data: List<T>,

    @Schema(example = "0", description = "page", type = "number", nullable = true)
    val page: Long? = null,

    @Schema(example = "20", description = "page size", type = "number", nullable = true)
    val size: Long? = null,

    val totalPages: Long? = null,
    val totalCount: Long? = null,
    val first: Boolean? = null,
    val last: Boolean? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiEnvelopCursorPage<T>(
    val data: List<T>,

    @Schema(description = "prev cursor", type = "string", nullable = true)
    val prevCursor: String? = null,

    @Schema(description = "next cursor", type = "string", nullable = true)
    val nextCursor: String? = null,
)
