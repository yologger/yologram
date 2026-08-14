package link.yologram.api.v1.domain.pms.tech.resource

import link.yologram.api.v1.config.security.AdminJwtProperties
import link.yologram.api.v1.config.security.JwtProperties
import link.yologram.api.v1.domain.pms.tech.exception.TechPostExceptionHandler
import link.yologram.api.v1.domain.pms.tech.exception.TechPostNotFoundException
import link.yologram.api.v1.domain.pms.tech.service.TechPostLikeService
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedAdminUserResolver
import link.yologram.api.v1.domain.ums.resolver.AuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.resolver.OptionalAuthenticatedUserResolver
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import link.yologram.api.v1.domain.ums.util.JwtUtil
import link.yologram.api.v1.global.exception.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post

@WebMvcTest(TechPostLikeResource::class)
@Import(
    TechPostExceptionHandler::class,
    GlobalExceptionHandler::class,
    AuthenticatedUserResolver::class,
    AuthenticatedAdminUserResolver::class,
    OptionalAuthenticatedUserResolver::class,
)
class TechPostLikeResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var likeService: TechPostLikeService

    @MockitoBean
    lateinit var jwtUtil: JwtUtil

    @MockitoBean
    lateinit var jwtProperties: JwtProperties

    @MockitoBean
    lateinit var adminJwtUtil: AdminJwtUtil

    @MockitoBean
    lateinit var adminJwtProperties: AdminJwtProperties

    @Test
    fun `좋아요 시 200을 반환하고 서비스에 위임한다`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(7L)

        mockMvc.post("/api/v1/pms/tech/posts/1/like") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isOk() }
        }

        verify(likeService).like(1L, 7L)
    }

    @Test
    fun `좋아요 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.post("/api/v1/pms/tech/posts/1/like")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
    }

    @Test
    fun `없는 글에 좋아요하면 404 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(7L)
        doThrow(TechPostNotFoundException()).whenever(likeService).like(any(), any())

        mockMvc.post("/api/v1/pms/tech/posts/9999/like") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.errorCode") { value("POST_NOT_FOUND") }
        }
    }

    @Test
    fun `좋아요 취소 시 200을 반환하고 서비스에 위임한다`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(7L)

        mockMvc.delete("/api/v1/pms/tech/posts/1/like") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isOk() }
        }

        verify(likeService).unlike(1L, 7L)
    }

    @Test
    fun `좋아요 취소 시 Authorization 헤더 없으면 401 반환`() {
        mockMvc.delete("/api/v1/pms/tech/posts/1/like")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("AUTH_INVALID_TOKEN") }
            }
    }

    @Test
    fun `없는 글의 좋아요를 취소하면 404 반환`() {
        whenever(jwtUtil.validateAndGetUid("valid-token")).thenReturn(7L)
        doThrow(TechPostNotFoundException()).whenever(likeService).unlike(any(), any())

        mockMvc.delete("/api/v1/pms/tech/posts/9999/like") {
            header("Authorization", "Bearer valid-token")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.errorCode") { value("POST_NOT_FOUND") }
        }
    }
}
