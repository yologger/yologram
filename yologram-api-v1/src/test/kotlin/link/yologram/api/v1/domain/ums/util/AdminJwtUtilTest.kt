package link.yologram.api.v1.domain.ums.util

import link.yologram.api.v1.config.security.AdminJwtProperties
import link.yologram.api.v1.config.security.JwtProperties
import link.yologram.api.v1.domain.ums.exception.AuthTokenExpiredException
import link.yologram.api.v1.domain.ums.exception.AuthTokenInvalidException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AdminJwtUtilTest {

    private val adminJwtProperties = AdminJwtProperties(
        secret = "test-admin-secret-key-for-unit-testing",
        expire = 3600,
        issuer = "yologram.link",
        audience = "yologram.admin",
    )

    private val adminJwtUtil = AdminJwtUtil(adminJwtProperties)

    @Nested
    inner class 토큰_생성 {

        @Test
        fun `토큰을 정상적으로 생성한다`() {
            val token = adminJwtUtil.createToken(1L)

            assertNotNull(token)
            assertTrue(token.isNotBlank())
        }
    }

    @Nested
    inner class 토큰_검증 {

        @Test
        fun `유효한 토큰에서 uid를 추출한다`() {
            val token = adminJwtUtil.createToken(1L)

            val uid = adminJwtUtil.validateAndGetUid(token)

            assertEquals(1L, uid)
        }

        @Test
        fun `만료된 토큰은 AuthTokenExpiredException을 던진다`() {
            val expiredAdminJwtUtil = AdminJwtUtil(adminJwtProperties.copy(expire = 0))
            val token = expiredAdminJwtUtil.createToken(1L)

            assertThrows<AuthTokenExpiredException> {
                adminJwtUtil.validateAndGetUid(token)
            }
        }

        @Test
        fun `잘못된 토큰은 AuthTokenInvalidException을 던진다`() {
            assertThrows<AuthTokenInvalidException> {
                adminJwtUtil.validateAndGetUid("invalid-token")
            }
        }

        @Test
        fun `다른 secret으로 서명된 토큰은 AuthTokenInvalidException을 던진다`() {
            val otherAdminJwtUtil = AdminJwtUtil(adminJwtProperties.copy(secret = "other-secret"))
            val token = otherAdminJwtUtil.createToken(1L)

            assertThrows<AuthTokenInvalidException> {
                adminJwtUtil.validateAndGetUid(token)
            }
        }
    }

    @Nested
    inner class 유저_토큰_혼용_차단 {

        @Test
        fun `유저용 JwtUtil로 생성한 토큰은 secret이 같아도 audience가 달라 거부된다`() {
            val userJwtUtil = JwtUtil(
                JwtProperties(
                    secret = adminJwtProperties.secret,
                    expire = 3600,
                    issuer = "yologram.link",
                    audience = "yologram.client",
                )
            )
            val userToken = userJwtUtil.createToken(1L)

            assertThrows<AuthTokenInvalidException> {
                adminJwtUtil.validateAndGetUid(userToken)
            }
        }

        @Test
        fun `어드민 토큰은 유저용 JwtUtil에서 거부된다`() {
            val userJwtUtil = JwtUtil(
                JwtProperties(
                    secret = adminJwtProperties.secret,
                    expire = 3600,
                    issuer = "yologram.link",
                    audience = "yologram.client",
                )
            )
            val adminToken = adminJwtUtil.createToken(1L)

            assertThrows<AuthTokenInvalidException> {
                userJwtUtil.validateAndGetUid(adminToken)
            }
        }
    }
}
