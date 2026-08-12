package link.yologram.api.v1.domain.pms.tech.resource

import com.fasterxml.jackson.databind.ObjectMapper
import link.yologram.api.v1.config.AdminJwtProperties
import link.yologram.api.v1.config.JwtProperties
import link.yologram.api.v1.domain.pms.tech.exception.InvalidTechCategoryException
import link.yologram.api.v1.domain.pms.tech.exception.InvalidTechPostCursorException
import link.yologram.api.v1.domain.pms.tech.exception.InvalidTechSectionException
import link.yologram.api.v1.domain.pms.tech.exception.TechPostExceptionHandler
import link.yologram.api.v1.domain.pms.tech.exception.TechPostForbiddenException
import link.yologram.api.v1.domain.pms.tech.exception.TechPostNotFoundException
import link.yologram.api.v1.domain.pms.tech.model.CreateTechPostRequest
import link.yologram.api.v1.domain.pms.tech.model.CreateTechPostResponse
import link.yologram.api.v1.domain.pms.tech.model.TechPostDetailResponse
import link.yologram.api.v1.domain.pms.tech.model.TechPostMetrics
import link.yologram.api.v1.domain.pms.tech.model.TechPostSummaryResponse
import link.yologram.api.v1.domain.pms.tech.model.UpdateTechPostRequest
import link.yologram.api.v1.domain.pms.tech.service.TechPostService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.resolver.OptionalAuthenticatedUserResolver
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
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
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

@WebMvcTest(TechPostResource::class)
@Import(
    TechPostExceptionHandler::class,
    ValidationExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
    AuthenticatedAdminUserResolver::class,
    OptionalAuthenticatedUserResolver::class,
)
class TechPostResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var postService: TechPostService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    @Test
    fun `정상 작성 시 201과 게시글 id를 반환한다`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        whenever(postService.create(any(), any())).thenReturn(CreateTechPostResponse(id = 10L))

        mockMvc.post("/api/v1/pms/tech/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTechPostRequest(content = "내용", categoryIds = listOf(1L)))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.id") { value(10) }
        }
    }

    @Test
    fun `Authorization 헤더 없으면 401 반환`() {
        mockMvc.post("/api/v1/pms/tech/posts") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTechPostRequest(content = "내용"))
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
            content = objectMapper.writeValueAsString(CreateTechPostRequest(content = "내용", categoryIds = emptyList()))
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
            content = objectMapper.writeValueAsString(CreateTechPostRequest(content = "내용", categoryIds = listOf(1L, 2L, 3L, 4L)))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `카테고리가 테크 게시판 것이 아니면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(InvalidTechCategoryException()).whenever(postService).create(any(), any())

        mockMvc.post("/api/v1/pms/tech/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTechPostRequest(content = "내용", categoryIds = listOf(99L)))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("INVALID_POST_CATEGORY") }
        }
    }

    @Test
    fun `tech가 아닌 섹션 경로는 매핑이 없어 404 반환`() {
        // 구 /pms/{section}/posts의 section 경로변수는 tech 고정 매핑으로 전환됨 — 그 외 섹션 경로는 미매핑(404)
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.post("/api/v1/pms/unknown/posts") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateTechPostRequest(content = "내용", categoryIds = listOf(1L)))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `게시글 수정 시 204 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.patch("/api/v1/pms/tech/posts/1") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTechPostRequest(title = "수정", content = "수정 내용", categoryIds = listOf(1L)))
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `게시글 수정 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.patch("/api/v1/pms/tech/posts/1") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTechPostRequest(content = "수정 내용", categoryIds = listOf(1L)))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
        }
    }

    @Test
    fun `본인 글이 아니면 403 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TechPostForbiddenException()).whenever(postService).update(any(), any(), any())

        mockMvc.patch("/api/v1/pms/tech/posts/1") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTechPostRequest(content = "수정 내용", categoryIds = listOf(1L)))
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.errorCode") { value("POST_FORBIDDEN") }
        }
    }

    @Test
    fun `존재하지 않는 글 수정 시 404 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TechPostNotFoundException()).whenever(postService).update(any(), any(), any())

        mockMvc.patch("/api/v1/pms/tech/posts/99") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateTechPostRequest(content = "수정 내용", categoryIds = listOf(1L)))
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.errorCode") { value("POST_NOT_FOUND") }
        }
    }

    @Test
    fun `수정 시 내용 누락이면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.patch("/api/v1/pms/tech/posts/1") {
            header("Authorization", "Bearer valid-token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("categoryIds" to listOf(1)))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("VALIDATION_ERROR") }
        }
    }

    @Test
    fun `게시글 삭제 시 204 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)

        mockMvc.delete("/api/v1/pms/tech/posts/1") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `게시글 삭제 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.delete("/api/v1/pms/tech/posts/1")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
    }

    @Test
    fun `본인 글이 아니면 삭제 시 403 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TechPostForbiddenException()).whenever(postService).delete(any(), any())

        mockMvc.delete("/api/v1/pms/tech/posts/1") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.errorCode") { value("POST_FORBIDDEN") }
        }
    }

    @Test
    fun `존재하지 않는 글 삭제 시 404 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(TechPostNotFoundException()).whenever(postService).delete(any(), any())

        mockMvc.delete("/api/v1/pms/tech/posts/99") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.errorCode") { value("POST_NOT_FOUND") }
        }
    }

    @Test
    fun `게시글 상세 조회 시 200과 게시글을 반환한다 (비로그인 - viewerUid null)`() {
        whenever(postService.getPost(eq(1L), isNull(), anyOrNull())).thenReturn(
            TechPostDetailResponse(
                id = 1L,
                author = TechPostDetailResponse.Author(uid = 12L, nickname = "tester"),
                title = "제목",
                content = "내용",
                categoryIds = listOf(1L, 2L),
                metrics = TechPostMetrics(commentCount = 2, likeCount = 5, likedByMe = false),
                createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            ),
        )

        mockMvc.get("/api/v1/pms/tech/posts/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.section") { value("TECH") }
                jsonPath("$.data.author.nickname") { value("tester") }
                jsonPath("$.data.content") { value("내용") }
                jsonPath("$.data.categoryIds[0]") { value(1) }
                jsonPath("$.data.metrics.commentCount") { value(2) }
                jsonPath("$.data.metrics.likeCount") { value(5) }
                jsonPath("$.data.metrics.likedByMe") { value(false) }
            }
    }

    @Test
    fun `로그인 상태로 상세 조회하면 viewerUid가 전달된다 (선택 인증)`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(7L)
        whenever(postService.getPost(eq(1L), eq(7L), anyOrNull())).thenReturn(
            TechPostDetailResponse(
                id = 1L,
                author = TechPostDetailResponse.Author(uid = 12L, nickname = "tester"),
                title = "제목",
                content = "내용",
                categoryIds = listOf(1L),
                metrics = TechPostMetrics(commentCount = 0, likeCount = 1, likedByMe = true),
                createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            ),
        )

        mockMvc.get("/api/v1/pms/tech/posts/1") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.metrics.likedByMe") { value(true) }
        }
    }

    @Test
    fun `상세 조회에 무효 토큰을 보내면 401 반환 (선택 인증은 헤더가 있으면 검증)`() {
        whenever(jwtUtil.validateAndGetUid("bad-token")).thenThrow(link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException())

        mockMvc.get("/api/v1/pms/tech/posts/1") {
            header("Authorization", "Bearer bad-token")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
        }
    }

    @Test
    fun `존재하지 않는 게시글이면 404 반환`() {
        whenever(postService.getPost(eq(99L), isNull(), anyOrNull())).thenThrow(TechPostNotFoundException())

        mockMvc.get("/api/v1/pms/tech/posts/99")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("POST_NOT_FOUND") }
            }
    }

    @Test
    fun `상세 조회 시 X-Forwarded-For 첫 값을 클라이언트 IP로 전달한다 (조회 이벤트용)`() {
        whenever(postService.getPost(eq(1L), isNull(), anyOrNull())).thenReturn(detailResponse())

        mockMvc.get("/api/v1/pms/tech/posts/1") {
            // 프록시 체인(API Gateway·CloudFront) — 맨 앞이 원 클라이언트
            header("X-Forwarded-For", "1.2.3.4, 70.41.3.18")
        }.andExpect {
            status { isOk() }
        }

        verify(postService).getPost(eq(1L), isNull(), eq("1.2.3.4"))
    }

    @Test
    fun `상세 조회 시 X-Forwarded-For가 없으면 remoteAddr를 전달한다`() {
        whenever(postService.getPost(eq(1L), isNull(), anyOrNull())).thenReturn(detailResponse())

        mockMvc.get("/api/v1/pms/tech/posts/1")
            .andExpect {
                status { isOk() }
            }

        // MockMvc 기본 remoteAddr
        verify(postService).getPost(eq(1L), isNull(), eq("127.0.0.1"))
    }

    private fun detailResponse() = TechPostDetailResponse(
        id = 1L,
        author = TechPostDetailResponse.Author(uid = 12L, nickname = "tester"),
        title = "제목",
        content = "내용",
        categoryIds = listOf(1L),
        metrics = TechPostMetrics(commentCount = 0, likeCount = 0, likedByMe = false),
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    @Test
    fun `게시글 목록 조회 시 200과 data·nextCursor를 반환한다`() {
        whenever(postService.getPostsByCursor(anyOrNull(), anyOrNull(), any(), anyOrNull())).thenReturn(
            ApiEnvelopCursorPage(
                data = listOf(
                    TechPostSummaryResponse(
                        id = 2L,
                        author = TechPostDetailResponse.Author(uid = 12L, nickname = "tester"),
                        title = "제목",
                        content = "내용",
                        categoryIds = listOf(1L),
                        metrics = TechPostMetrics(commentCount = 3, likeCount = 1, likedByMe = false),
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
                jsonPath("$.data[0].section") { value("TECH") }
                jsonPath("$.data[0].author.nickname") { value("tester") }
                jsonPath("$.data[0].metrics.commentCount") { value(3) }
                jsonPath("$.data[0].metrics.likeCount") { value(1) }
                jsonPath("$.data[0].metrics.likedByMe") { value(false) }
                jsonPath("$.nextCursor") { value("next-cursor") }
            }
    }

    @Test
    fun `목록 조회 시 유효하지 않은 커서면 400 반환`() {
        doThrow(InvalidTechPostCursorException()).whenever(postService).getPostsByCursor(anyOrNull(), eq("@@@"), any(), anyOrNull())

        mockMvc.get("/api/v1/pms/tech/posts?cursor=@@@")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.errorCode") { value("INVALID_CURSOR") }
            }
    }

    @Test
    fun `tech가 아닌 섹션 피드 경로는 매핑이 없어 404 반환`() {
        mockMvc.get("/api/v1/pms/unknown/posts")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `내 글 목록 조회(cursor) 시 200과 data·nextCursor를 반환한다`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        whenever(postService.getMyPostsByCursor(eq(1L), anyOrNull(), anyOrNull(), any())).thenReturn(
            ApiEnvelopCursorPage(
                data = listOf(
                    TechPostSummaryResponse(
                        id = 5L,
                        author = TechPostDetailResponse.Author(uid = 1L, nickname = "me"),
                        title = "제목",
                        content = "내용",
                        categoryIds = listOf(1L),
                        metrics = TechPostMetrics(commentCount = 0, likeCount = 0, likedByMe = false),
                        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                    ),
                ),
                nextCursor = "next-cursor",
            ),
        )

        mockMvc.get("/api/v1/pms/posts/me") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(5) }
            jsonPath("$.data[0].author.nickname") { value("me") }
            jsonPath("$.nextCursor") { value("next-cursor") }
        }
    }

    @Test
    fun `내 글 목록 조회 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.get("/api/v1/pms/posts/me")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
    }

    @Test
    fun `내 글 목록 조회 시 유효하지 않은 section이면 400 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(1L)
        doThrow(InvalidTechSectionException()).whenever(postService).getMyPostsByCursor(eq(1L), eq("unknown"), anyOrNull(), any())

        mockMvc.get("/api/v1/pms/posts/me?section=unknown") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errorCode") { value("INVALID_SECTION") }
        }
    }
}
