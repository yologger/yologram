package link.yologram.api.v1.domain.pms.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.cms.exception.CmsExceptionHandler
import link.yologram.api.v1.domain.cms.exception.InvalidSectionException
import link.yologram.api.v1.domain.pms.exception.InvalidPostCategoryException
import link.yologram.api.v1.domain.pms.exception.PmsExceptionHandler
import link.yologram.api.v1.domain.cms.enums.Section
import link.yologram.api.v1.domain.pms.exception.PostNotFoundException
import link.yologram.api.v1.domain.pms.model.CreatePostRequest
import link.yologram.api.v1.domain.pms.model.CreatePostResponse
import link.yologram.api.v1.domain.pms.model.PostDetailResponse
import link.yologram.api.v1.domain.pms.model.PostSummaryResponse
import link.yologram.api.v1.domain.pms.service.PostService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@WebMvcTest(PostResource::class)
@Import(
    PmsExceptionHandler::class,
    CmsExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
)
class PostResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var postService: PostService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @Test
    fun `정상 작성 시 201과 게시글 id를 반환한다`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        whenever(postService.create(any(), any(), any())).thenReturn(CreatePostResponse(id = 10L))

        mockMvc.post("/api/v1/pms/tech/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = "내용", categoryIds = listOf(1L)))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.id") { value(10) }
        }
    }

    @Test
    fun `Authorization 헤더 없으면 401 반환`() {
        mockMvc.post("/api/v1/pms/tech/posts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = "내용"))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
        }
    }

    @Test
    fun `내용 누락 시 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.post("/api/v1/pms/tech/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("categoryIds" to listOf(1)))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `카테고리 미선택(빈 배열) 시 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.post("/api/v1/pms/tech/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = "내용", categoryIds = emptyList()))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `카테고리 4개 이상이면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.post("/api/v1/pms/tech/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = "내용", categoryIds = listOf(1L, 2L, 3L, 4L)))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `카테고리가 해당 section 것이 아니면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(InvalidPostCategoryException()).whenever(postService).create(any(), any(), any())

        mockMvc.post("/api/v1/pms/tech/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = "내용", categoryIds = listOf(99L)))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("INVALID_POST_CATEGORY") }
        }
    }

    @Test
    fun `유효하지 않은 section이면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(InvalidSectionException()).whenever(postService).create(any(), any(), any())

        mockMvc.post("/api/v1/pms/unknown/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreatePostRequest(content = "내용", categoryIds = listOf(1L)))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("INVALID_SECTION") }
        }
    }

    @Test
    fun `게시글 상세 조회 시 200과 게시글을 반환한다`() {
        whenever(postService.getPost("tech", 1L)).thenReturn(
            PostDetailResponse(
                id = 1L,
                section = Section.TECH,
                author = PostDetailResponse.Author(uid = 12L, nickname = "tester"),
                title = "제목",
                content = "내용",
                categoryIds = listOf(1L, 2L),
                likeCount = 0,
                commentCount = 0,
                createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            ),
        )

        mockMvc.get("/api/v1/pms/tech/posts/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.author.nickname") { value("tester") }
                jsonPath("$.data.content") { value("내용") }
                jsonPath("$.data.categoryIds[0]") { value(1) }
            }
    }

    @Test
    fun `존재하지 않는 게시글이면 404 반환`() {
        whenever(postService.getPost("tech", 99L)).thenThrow(PostNotFoundException())

        mockMvc.get("/api/v1/pms/tech/posts/99")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("POST_NOT_FOUND") }
            }
    }

    @Test
    fun `게시글 목록 조회 시 200과 data·nextCursor를 반환한다`() {
        whenever(postService.getPosts(eq("tech"), anyOrNull(), anyOrNull(), any())).thenReturn(
            ApiEnvelopCursorPage(
                data = listOf(
                    PostSummaryResponse(
                        id = 2L,
                        section = Section.TECH,
                        author = PostDetailResponse.Author(uid = 12L, nickname = "tester"),
                        title = "제목",
                        content = "내용",
                        categoryIds = listOf(1L),
                        likeCount = 0,
                        commentCount = 0,
                        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                    ),
                ),
                nextCursor = "next-cursor",
            ),
        )

        mockMvc.get("/api/v1/pms/tech/posts")
            .andExpect {
                status { isOk() }
                jsonPath("$.data[0].id") { value(2) }
                jsonPath("$.data[0].author.nickname") { value("tester") }
                jsonPath("$.nextCursor") { value("next-cursor") }
            }
    }

    @Test
    fun `목록 조회 시 유효하지 않은 section이면 400 반환`() {
        doThrow(InvalidSectionException()).whenever(postService).getPosts(eq("unknown"), anyOrNull(), anyOrNull(), any())

        mockMvc.get("/api/v1/pms/unknown/posts")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_SECTION") }
            }
    }
}
