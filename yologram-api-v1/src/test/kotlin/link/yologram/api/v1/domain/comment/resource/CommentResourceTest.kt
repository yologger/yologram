package link.yologram.api.v1.domain.comment.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.comment.exception.CommentExceptionHandler
import link.yologram.api.v1.domain.comment.exception.TargetPostNotFoundException
import link.yologram.api.v1.domain.comment.model.CreateCommentRequest
import link.yologram.api.v1.domain.comment.model.CreateCommentResponse
import link.yologram.api.v1.domain.comment.service.CommentService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import link.yologram.api.v1.global.exception.ValidationExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(CommentResource::class)
@Import(
    CommentExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
)
class CommentResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var commentService: CommentService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @Test
    fun `정상 작성 시 201과 댓글 id를 반환한다`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        whenever(commentService.create(any(), any(), any())).thenReturn(CreateCommentResponse(id = 10L))

        mockMvc.post("/api/v1/comments/posts/100") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateCommentRequest(content = "좋은 글"))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.id") { value(10) }
        }
    }

    @Test
    fun `작성 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.post("/api/v1/comments/posts/100") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateCommentRequest(content = "좋은 글"))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
        }
    }

    @Test
    fun `작성 시 내용 누락이면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.post("/api/v1/comments/posts/100") {
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
        doThrow(TargetPostNotFoundException()).whenever(commentService).create(any(), any(), any())

        mockMvc.post("/api/v1/comments/posts/999") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateCommentRequest(content = "좋은 글"))
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.errorCode") { value("POST_NOT_FOUND") }
        }
    }
}
