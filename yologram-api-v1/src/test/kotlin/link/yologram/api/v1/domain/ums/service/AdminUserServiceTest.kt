package link.yologram.api.v1.domain.ums.service

import link.yologram.api.v1.domain.ums.entity.AdminUser
import link.yologram.api.v1.domain.ums.enum.AdminUserRole
import link.yologram.api.v1.domain.ums.enum.UserStatus
import link.yologram.api.v1.domain.ums.exception.AdminRoleForbiddenException
import link.yologram.api.v1.domain.ums.exception.AdminUserDuplicateException
import link.yologram.api.v1.domain.ums.exception.AdminUserInactiveException
import link.yologram.api.v1.domain.ums.exception.AdminUserNotFoundException
import link.yologram.api.v1.domain.ums.exception.AdminUserOwnerImmutableException
import link.yologram.api.v1.domain.ums.exception.AdminUserOwnerUndeletableException
import link.yologram.api.v1.domain.ums.exception.AdminUserSelfDeleteException
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.transaction.annotation.Transactional
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
        role: AdminUserRole = AdminUserRole.ADMIN,
        status: UserStatus = UserStatus.ACTIVE,
    ) = AdminUser(id = id, email = email, name = name, password = "encoded-password", role = role, status = status)

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

        @Test
        fun `role은 항상 ADMIN으로 생성된다`() {
            val request = createRequest()

            whenever(adminUserRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded-password")
            whenever(adminUserRepository.save(any<AdminUser>())).thenAnswer {
                val admin = it.arguments[0] as AdminUser
                assertEquals(AdminUserRole.ADMIN, admin.role)
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
            assertEquals(AdminUserRole.ADMIN, response.role)
        }

        @Test
        fun `OWNER 계정 로그인 시 role OWNER를 반환한다`() {
            val request = AdminLoginRequest(email = "owner@yologram.link", password = "password123")
            val owner = testAdminUser(email = "owner@yologram.link", role = AdminUserRole.OWNER)

            whenever(adminUserRepository.findByEmail(request.email)).thenReturn(Optional.of(owner))
            whenever(passwordEncoder.matches(request.password, owner.password)).thenReturn(true)
            whenever(adminJwtUtil.createToken(owner.id)).thenReturn("owner-token")

            val response = adminUserService.login(request)

            assertEquals(AdminUserRole.OWNER, response.role)
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

        @Test
        fun `INACTIVE 계정이면 AdminUserInactiveException 발생`() {
            val request = AdminLoginRequest(email = "admin@yologram.link", password = "password123")
            val admin = testAdminUser(status = UserStatus.INACTIVE)

            whenever(adminUserRepository.findByEmail(request.email)).thenReturn(Optional.of(admin))
            whenever(passwordEncoder.matches(request.password, admin.password)).thenReturn(true)

            val exception = assertThrows<AdminUserInactiveException> {
                adminUserService.login(request)
            }

            assertEquals("ADMIN_USER_INACTIVE", exception.errorCode)
            verify(adminJwtUtil, never()).createToken(any())
        }

        @Test
        fun `INACTIVE 계정이라도 비밀번호가 틀리면 AuthWrongPasswordException이 먼저다`() {
            val request = AdminLoginRequest(email = "admin@yologram.link", password = "wrongpass")
            val admin = testAdminUser(status = UserStatus.INACTIVE)

            whenever(adminUserRepository.findByEmail(request.email)).thenReturn(Optional.of(admin))
            whenever(passwordEncoder.matches(request.password, admin.password)).thenReturn(false)

            val exception = assertThrows<AuthWrongPasswordException> {
                adminUserService.login(request)
            }

            assertEquals("AUTH_WRONG_PASSWORD", exception.errorCode)
        }
    }

    @Nested
    inner class 토큰_검증 {

        @Test
        fun `토큰 검증은 readOnly true 트랜잭션을 사용한다`() {
            val method = AdminUserService::class.java.getMethod("validateToken", String::class.java)
            val transactional = method.getAnnotation(Transactional::class.java)

            assertNotNull(transactional)
            assertTrue(transactional.readOnly)
        }

        @Test
        fun `JWT가 유효하면 어드민 정보를 반환한다`() {
            val admin = testAdminUser()

            whenever(adminJwtUtil.validateAndGetUid("valid-admin-token")).thenReturn(1L)
            whenever(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin))

            val result = adminUserService.validateToken("valid-admin-token")

            assertEquals(1L, result.uid)
            assertEquals("admin@yologram.link", result.email)
            assertEquals("어드민", result.name)
            assertEquals(AdminUserRole.ADMIN, result.role)
        }

        @Test
        fun `존재하지 않는 어드민이면 AdminUserNotFoundException을 던진다`() {
            whenever(adminJwtUtil.validateAndGetUid("valid-admin-token")).thenReturn(999L)
            whenever(adminUserRepository.findById(999L)).thenReturn(Optional.empty())

            val exception = assertThrows<AdminUserNotFoundException> {
                adminUserService.validateToken("valid-admin-token")
            }

            assertEquals("ADMIN_USER_NOT_FOUND", exception.errorCode)
        }

        @Test
        fun `INACTIVE 계정이면 AdminUserInactiveException을 던진다`() {
            val inactive = testAdminUser(status = UserStatus.INACTIVE)

            whenever(adminJwtUtil.validateAndGetUid("valid-admin-token")).thenReturn(1L)
            whenever(adminUserRepository.findById(1L)).thenReturn(Optional.of(inactive))

            val exception = assertThrows<AdminUserInactiveException> {
                adminUserService.validateToken("valid-admin-token")
            }

            assertEquals("ADMIN_USER_INACTIVE", exception.errorCode)
        }
    }

    @Nested
    inner class 어드민_상태_변경 {

        private fun owner(id: Long = 1L) =
            testAdminUser(id = id, email = "owner@yologram.link", name = "오너", role = AdminUserRole.OWNER)

        @Test
        fun `OWNER가 ADMIN을 INACTIVE로 비활성화한다`() {
            val target = testAdminUser(id = 2L, email = "target@yologram.link")
            whenever(adminUserRepository.findById(1L)).thenReturn(Optional.of(owner()))
            whenever(adminUserRepository.findById(2L)).thenReturn(Optional.of(target))
            whenever(adminUserRepository.saveAndFlush(any<AdminUser>())).thenAnswer { it.arguments[0] }

            val result = adminUserService.updateStatus(1L, 2L, UserStatus.INACTIVE)

            assertEquals(2L, result.uid)
            assertEquals(UserStatus.INACTIVE, result.status)
        }

        @Test
        fun `OWNER가 INACTIVE ADMIN을 다시 ACTIVE로 전환한다`() {
            val target = testAdminUser(id = 2L, email = "target@yologram.link", status = UserStatus.INACTIVE)
            whenever(adminUserRepository.findById(1L)).thenReturn(Optional.of(owner()))
            whenever(adminUserRepository.findById(2L)).thenReturn(Optional.of(target))
            whenever(adminUserRepository.saveAndFlush(any<AdminUser>())).thenAnswer { it.arguments[0] }

            val result = adminUserService.updateStatus(1L, 2L, UserStatus.ACTIVE)

            assertEquals(UserStatus.ACTIVE, result.status)
        }

        @Test
        fun `요청자가 OWNER가 아니면 AdminRoleForbiddenException 발생`() {
            whenever(adminUserRepository.findById(1L)).thenReturn(Optional.of(testAdminUser(id = 1L)))

            val exception = assertThrows<AdminRoleForbiddenException> {
                adminUserService.updateStatus(1L, 2L, UserStatus.INACTIVE)
            }

            assertEquals("ADMIN_ROLE_FORBIDDEN", exception.errorCode)
            verify(adminUserRepository, never()).findById(2L)
            verify(adminUserRepository, never()).saveAndFlush(any<AdminUser>())
        }

        @Test
        fun `대상이 OWNER면 AdminUserOwnerImmutableException 발생`() {
            whenever(adminUserRepository.findById(1L)).thenReturn(Optional.of(owner(id = 1L)))
            whenever(adminUserRepository.findById(2L)).thenReturn(Optional.of(owner(id = 2L)))

            val exception = assertThrows<AdminUserOwnerImmutableException> {
                adminUserService.updateStatus(1L, 2L, UserStatus.INACTIVE)
            }

            assertEquals("ADMIN_USER_OWNER_IMMUTABLE", exception.errorCode)
            verify(adminUserRepository, never()).saveAndFlush(any<AdminUser>())
        }

        @Test
        fun `대상이 없으면 AdminUserNotFoundException 발생`() {
            whenever(adminUserRepository.findById(1L)).thenReturn(Optional.of(owner()))
            whenever(adminUserRepository.findById(999L)).thenReturn(Optional.empty())

            val exception = assertThrows<AdminUserNotFoundException> {
                adminUserService.updateStatus(1L, 999L, UserStatus.INACTIVE)
            }

            assertEquals("ADMIN_USER_NOT_FOUND", exception.errorCode)
        }

        @Test
        fun `요청자가 없으면 AdminUserNotFoundException 발생`() {
            whenever(adminUserRepository.findById(999L)).thenReturn(Optional.empty())

            val exception = assertThrows<AdminUserNotFoundException> {
                adminUserService.updateStatus(999L, 2L, UserStatus.INACTIVE)
            }

            assertEquals("ADMIN_USER_NOT_FOUND", exception.errorCode)
            verify(adminUserRepository, never()).findById(2L)
        }
    }

    @Nested
    inner class 로그아웃 {

        @Test
        fun `로그아웃은 아무 동작 없이 성공한다`() {
            adminUserService.logout(1L)
        }
    }

    @Nested
    inner class 어드민_목록_조회 {

        private fun admins(vararg ids: Long) =
            ids.map { testAdminUser(id = it, email = "admin$it@yologram.link", name = "어드민$it") }

        @Test
        fun `첫 페이지 조회 시 id 오름차순 Pageable로 조회하고 페이지 메타를 채운다`() {
            whenever(adminUserRepository.findAll(any<Pageable>())).thenReturn(
                PageImpl(admins(1L, 2L), PageRequest.of(0, 2), 5)
            )

            val result = adminUserService.getAdminUsers(0, 2)

            val captor = argumentCaptor<Pageable>()
            verify(adminUserRepository).findAll(captor.capture())
            assertEquals(0, captor.firstValue.pageNumber)
            assertEquals(2, captor.firstValue.pageSize)
            assertEquals(Sort.by("id").ascending(), captor.firstValue.sort)

            assertEquals(listOf(1L, 2L), result.data.map { it.uid })
            assertEquals("admin1@yologram.link", result.data[0].email)
            assertEquals("어드민1", result.data[0].name)
            assertEquals(UserStatus.ACTIVE, result.data[0].status)
            assertNotNull(result.data[0].joinedDate)
            assertEquals(0L, result.page)
            assertEquals(2L, result.size)
            assertEquals(3L, result.totalPages)
            assertEquals(5L, result.totalCount)
            assertEquals(true, result.first)
            assertEquals(false, result.last)
        }

        @Test
        fun `두 번째 페이지 조회 시 first는 false다`() {
            whenever(adminUserRepository.findAll(any<Pageable>())).thenReturn(
                PageImpl(admins(3L, 4L), PageRequest.of(1, 2), 5)
            )

            val result = adminUserService.getAdminUsers(1, 2)

            assertEquals(listOf(3L, 4L), result.data.map { it.uid })
            assertEquals(1L, result.page)
            assertEquals(false, result.first)
            assertEquals(false, result.last)
        }

        @Test
        fun `마지막 페이지 조회 시 last는 true다`() {
            whenever(adminUserRepository.findAll(any<Pageable>())).thenReturn(
                PageImpl(admins(5L), PageRequest.of(2, 2), 5)
            )

            val result = adminUserService.getAdminUsers(2, 2)

            assertEquals(listOf(5L), result.data.map { it.uid })
            assertEquals(2L, result.page)
            assertEquals(3L, result.totalPages)
            assertEquals(false, result.first)
            assertEquals(true, result.last)
        }

        @Test
        fun `범위 밖 페이지 조회 시 빈 데이터와 전체 메타를 반환한다`() {
            whenever(adminUserRepository.findAll(any<Pageable>())).thenReturn(
                PageImpl(emptyList(), PageRequest.of(9, 10), 2)
            )

            val result = adminUserService.getAdminUsers(9, 10)

            assertTrue(result.data.isEmpty())
            assertEquals(9L, result.page)
            assertEquals(1L, result.totalPages)
            assertEquals(2L, result.totalCount)
        }
    }

    @Nested
    inner class 어드민_삭제 {

        @Test
        fun `삭제 성공 시 repository delete가 호출된다`() {
            val target = testAdminUser(id = 2L, email = "target@yologram.link")
            whenever(adminUserRepository.findById(2L)).thenReturn(Optional.of(target))

            adminUserService.delete(1L, 2L)

            verify(adminUserRepository).delete(target)
        }

        @Test
        fun `자기 자신 삭제 시 AdminUserSelfDeleteException 발생`() {
            val exception = assertThrows<AdminUserSelfDeleteException> {
                adminUserService.delete(1L, 1L)
            }

            assertEquals("ADMIN_USER_SELF_DELETE", exception.errorCode)
            verify(adminUserRepository, never()).findById(any())
            verify(adminUserRepository, never()).delete(any<AdminUser>())
        }

        @Test
        fun `없는 id면 AdminUserNotFoundException 발생`() {
            whenever(adminUserRepository.findById(999L)).thenReturn(Optional.empty())

            val exception = assertThrows<AdminUserNotFoundException> {
                adminUserService.delete(1L, 999L)
            }

            assertEquals("ADMIN_USER_NOT_FOUND", exception.errorCode)
            verify(adminUserRepository, never()).delete(any<AdminUser>())
        }

        @Test
        fun `대상이 OWNER면 AdminUserOwnerUndeletableException 발생`() {
            val owner = testAdminUser(id = 2L, email = "owner@yologram.link", role = AdminUserRole.OWNER)
            whenever(adminUserRepository.findById(2L)).thenReturn(Optional.of(owner))

            val exception = assertThrows<AdminUserOwnerUndeletableException> {
                adminUserService.delete(1L, 2L)
            }

            assertEquals("ADMIN_USER_OWNER_UNDELETABLE", exception.errorCode)
            verify(adminUserRepository, never()).delete(any<AdminUser>())
        }

        @Test
        fun `자기 자신이 OWNER 대상이어도 자기 자신 검사가 먼저다`() {
            val exception = assertThrows<AdminUserSelfDeleteException> {
                adminUserService.delete(2L, 2L)
            }

            assertEquals("ADMIN_USER_SELF_DELETE", exception.errorCode)
            verify(adminUserRepository, never()).findById(any())
        }
    }
}
