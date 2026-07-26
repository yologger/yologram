package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.AdminUser
import link.yologram.api.v1.domain.ums.enum.UserStatus
import link.yologram.api.v1.domain.ums.exception.AdminUserDuplicateException
import link.yologram.api.v1.domain.ums.exception.AdminUserNotFoundException
import link.yologram.api.v1.domain.ums.exception.AuthWrongPasswordException
import link.yologram.api.v1.domain.ums.model.AdminLoginRequest
import link.yologram.api.v1.domain.ums.model.AdminUserCreateRequest
import link.yologram.api.v1.domain.ums.repository.AdminUserRepository
import link.yologram.api.v1.domain.ums.util.AdminJwtUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminUserServiceTest {

    @Mock
    lateinit var adminUserRepository: AdminUserRepository

    @Mock
    lateinit var passwordEncoder: BCryptPasswordEncoder

    @Mock
    lateinit var adminJwtUtil: AdminJwtUtil

    @InjectMocks
    lateinit var adminUserService: AdminUserService

    private fun createRequest(
        email: String = "admin@yologram.link",
        name: String = "어드민",
        password: String = "password123",
    ) = AdminUserCreateRequest(email = email, name = name, password = password)

    private fun testAdminUser(
        id: Long = 1L,
        email: String = "admin@yologram.link",
        name: String = "어드민",
    ) = AdminUser(id = id, email = email, name = name, password = "encoded-password")

    @Nested
    inner class 어드민_생성_성공 {

        @Test
        fun `uid를 반환한다`() {
            val request = createRequest()

            whenever(adminUserRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(adminUserRepository.save(any<AdminUser>())).thenReturn(testAdminUser())

            val response = adminUserService.create(request)

            assertEquals(1L, response.uid)
        }

        @Test
        fun `비밀번호가 암호화된다`() {
            val request = createRequest()

            whenever(adminUserRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(adminUserRepository.save(any<AdminUser>())).thenAnswer { it.arguments[0] as AdminUser }

            adminUserService.create(request)

            verify(passwordEncoder).encode(request.password)
        }

        @Test
        fun `기본 상태는 ACTIVE`() {
            val request = createRequest()

            whenever(adminUserRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(adminUserRepository.save(any<AdminUser>())).thenAnswer {
                val admin = it.arguments[0] as AdminUser
                assertEquals(UserStatus.ACTIVE, admin.status)
                admin
            }

            adminUserService.create(request)
        }
    }

    @Nested
    inner class 어드민_생성_실패 {

        @Test
        fun `중복 이메일 시 AdminUserDuplicateException 발생`() {
            val request = createRequest()

            whenever(adminUserRepository.existsByEmail(request.email)).thenReturn(true)

            val exception = assertThrows<AdminUserDuplicateException> {
                adminUserService.create(request)
            }

            assertEquals("ADMIN_USER_DUPLICATE", exception.errorCode)
        }
    }

    @Nested
    inner class 로그인_성공 {

        @Test
        fun `accessToken과 어드민 정보를 반환한다`() {
            val request = AdminLoginRequest(email = "admin@yologram.link", password = "password123")
            val admin = testAdminUser()

            whenever(adminUserRepository.findByEmail(request.email)).thenReturn(Optional.of(admin))
            whenever(passwordEncoder.matches(request.password, admin.password)).thenReturn(true)
            whenever(adminJwtUtil.createToken(admin.id)).thenReturn("admin-token")

            val response = adminUserService.login(request)

            assertEquals(1L, response.uid)
            assertEquals("admin-token", response.accessToken)
            assertEquals("admin@yologram.link", response.email)
            assertEquals("어드민", response.name)
        }
    }

    @Nested
    inner class 로그인_실패 {

        @Test
        fun `존재하지 않는 이메일 시 AdminUserNotFoundException 발생`() {
            val request = AdminLoginRequest(email = "notfound@yologram.link", password = "password123")

            whenever(adminUserRepository.findByEmail(request.email)).thenReturn(Optional.empty())

            val exception = assertThrows<AdminUserNotFoundException> {
                adminUserService.login(request)
            }

            assertEquals("ADMIN_USER_NOT_FOUND", exception.errorCode)
        }

        @Test
        fun `비밀번호 불일치 시 AuthWrongPasswordException 발생`() {
            val request = AdminLoginRequest(email = "admin@yologram.link", password = "wrongpass")
            val admin = testAdminUser()

            whenever(adminUserRepository.findByEmail(request.email)).thenReturn(Optional.of(admin))
            whenever(passwordEncoder.matches(request.password, admin.password)).thenReturn(false)

            val exception = assertThrows<AuthWrongPasswordException> {
                adminUserService.login(request)
            }

            assertEquals("AUTH_WRONG_PASSWORD", exception.errorCode)
        }
    }
}
