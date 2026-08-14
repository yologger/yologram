package link.yologram.api.v1.domain.ums.util

import link.yologram.api.v1.config.security.JwtProperties
import link.yologram.api.v1.domain.ums.exception.AuthTokenExpiredException
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JwtUtilTest {

    private val jwtProperties = JwtProperties(
        secret = "test-secret-key-for-unit-testing",
        expire = 3600,
        issuer = "yologram.link",
        audience = "yologram.client",
    )

    private val jwtUtil = JwtUtil(jwtProperties)

    @Nested
    inner class 토큰_생성 {

        @Test
        fun `토큰을 정상적으로 생성한다`() {
            val token = jwtUtil.createToken(1L)

            assertNotNull(token)
            assertTrue(token.isNotBlank())
        }
    }

    @Nested
    inner class 토큰_검증 {

        @Test
        fun `유효한 토큰에서 uid를 추출한다`() {
            val token = jwtUtil.createToken(1L)

            val uid = jwtUtil.validateAndGetUid(token)

            assertEquals(1L, uid)
        }

        @Test
        fun `만료된 토큰은 AuthTokenExpiredException을 던진다`() {
            val expiredJwtUtil = JwtUtil(jwtProperties.copy(expire = 0))
            val token = expiredJwtUtil.createToken(1L)

            assertThrows<AuthTokenExpiredException> {
                jwtUtil.validateAndGetUid(token)
            }
        }

        @Test
        fun `잘못된 토큰은 AuthTokenInvalidException을 던진다`() {
            assertThrows<AuthTokenInvalidException> {
                jwtUtil.validateAndGetUid("invalid-token")
            }
        }

        @Test
        fun `다른 secret으로 서명된 토큰은 AuthTokenInvalidException을 던진다`() {
            val otherJwtUtil = JwtUtil(jwtProperties.copy(secret = "other-secret"))
            val token = otherJwtUtil.createToken(1L)

            assertThrows<AuthTokenInvalidException> {
                jwtUtil.validateAndGetUid(token)
            }
        }
    }
}
