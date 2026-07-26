package link.yologram.api.v1.domain.tech.comment.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.AdminJwtProperties
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.tech.comment.exception.InvalidTechPostCommentCursorException
import link.yologram.api.v1.domain.tech.comment.exception.TargetTechPostNotFoundException
import link.yologram.api.v1.domain.tech.comment.exception.TechPostCommentExceptionHandler
import link.yologram.api.v1.domain.tech.comment.exception.TechPostCommentForbiddenException
import link.yologram.api.v1.domain.tech.comment.exception.TechPostCommentNotFoundException
import link.yologram.api.v1.domain.tech.comment.model.CreateTechPostCommentRequest
import link.yologram.api.v1.domain.tech.comment.model.CreateTechPostCommentResponse
import link.yologram.api.v1.domain.tech.comment.model.TechPostCommentResponse
import link.yologram.api.v1.domain.tech.comment.model.UpdateTechPostCommentRequest
import link.yologram.api.v1.domain.tech.comment.service.TechPostCommentService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import link.yologram.api.v1.global.exception.ValidationExceptionHandler
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@WebMvcTest(TechPostCommentResource::class)
@Import(
    TechPostCommentExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
    AuthenticatedAdminUserResolver::class,
)
class TechPostCommentResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var commentService: TechPostCommentService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    @Test
    fun `정상 작성 시 201과 댓글 id를 반환한다`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        whenever(commentService.create(any(), any(), any())).thenReturn(CreateTechPostCommentResponse(id = 10L))

        mockMvc.post("/api/v1/comments/tech/posts/100") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTechPostCommentRequest(content = "좋은 글"))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.id") { value(10) }
        }
    }

    @Test
    fun `작성 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.post("/api/v1/comments/tech/posts/100") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTechPostCommentRequest(content = "좋은 글"))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
        }
    }

    @Test
    fun `작성 시 내용 누락이면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.post("/api/v1/comments/tech/posts/100") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf<String, Any>())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `대상 게시글이 없으면 404 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TargetTechPostNotFoundException()).whenever(commentService).create(any(), any(), any())

        mockMvc.post("/api/v1/comments/tech/posts/999") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTechPostCommentRequest(content = "좋은 글"))
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.errorCode") { value("POST_NOT_FOUND") }
        }
    }

    @Test
    fun `댓글 수정 시 204 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.patch("/api/v1/comments/tech/1") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTechPostCommentRequest(content = "수정된 댓글"))
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `수정 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.patch("/api/v1/comments/tech/1") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTechPostCommentRequest(content = "수정된 댓글"))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
        }
    }

    @Test
    fun `본인 댓글이 아니면 수정 시 403 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TechPostCommentForbiddenException()).whenever(commentService).update(any(), any(), any())

        mockMvc.patch("/api/v1/comments/tech/1") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTechPostCommentRequest(content = "수정된 댓글"))
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.errorCode") { value("COMMENT_FORBIDDEN") }
        }
    }

    @Test
    fun `존재하지 않는 댓글 수정 시 404 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TechPostCommentNotFoundException()).whenever(commentService).update(any(), any(), any())

        mockMvc.patch("/api/v1/comments/tech/99") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTechPostCommentRequest(content = "수정된 댓글"))
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.errorCode") { value("COMMENT_NOT_FOUND") }
        }
    }

    @Test
    fun `수정 시 내용 누락이면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.patch("/api/v1/comments/tech/1") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf<String, Any>())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `댓글 삭제 시 204 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.delete("/api/v1/comments/tech/1") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `삭제 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.delete("/api/v1/comments/tech/1")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
    }

    @Test
    fun `본인 댓글이 아니면 삭제 시 403 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TechPostCommentForbiddenException()).whenever(commentService).delete(any(), any())

        mockMvc.delete("/api/v1/comments/tech/1") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.errorCode") { value("COMMENT_FORBIDDEN") }
        }
    }

    @Test
    fun `존재하지 않는 댓글 삭제 시 404 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TechPostCommentNotFoundException()).whenever(commentService).delete(any(), any())

        mockMvc.delete("/api/v1/comments/tech/99") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.errorCode") { value("COMMENT_NOT_FOUND") }
        }
    }

    @Test
    fun `댓글 목록 조회 시 200과 data·nextCursor를 반환한다`() {
        whenever(commentService.getCommentsByCursor(eq(1155L), anyOrNull(), anyOrNull(), any())).thenReturn(
            ApiEnvelopCursorPage(
                data = listOf(
                    TechPostCommentResponse(
                        id = 2L,
                        postId = 1155L,
                        author = TechPostCommentResponse.Author(uid = 12L, nickname = "tester"),
                        content = "댓글 내용",
                        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                    ),
                ),
                nextCursor = "next-cursor",
            ),
        )

        mockMvc.get("/api/v1/comments/tech/posts/1155")
            .andExpect {
                status { isOk() }
                jsonPath("$.data[0].id") { value(2) }
                jsonPath("$.data[0].author.nickname") { value("tester") }
                jsonPath("$.data[0].content") { value("댓글 내용") }
                jsonPath("$.nextCursor") { value("next-cursor") }
            }
    }

    @Test
    fun `댓글 목록 조회는 인증 없이 가능하다`() {
        whenever(commentService.getCommentsByCursor(eq(1155L), anyOrNull(), anyOrNull(), any())).thenReturn(
            ApiEnvelopCursorPage(data = emptyList(), nextCursor = null),
        )

        mockMvc.get("/api/v1/comments/tech/posts/1155")
            .andExpect {
                status { isOk() }
                jsonPath("$.data") { isEmpty() }
            }
    }

    @Test
    fun `유효하지 않은 커서면 400 반환`() {
        doThrow(InvalidTechPostCommentCursorException()).whenever(commentService).getCommentsByCursor(eq(1155L), anyOrNull(), anyOrNull(), any())

        mockMvc.get("/api/v1/comments/tech/posts/1155?cursor=@@@")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_CURSOR") }
            }
    }
}
