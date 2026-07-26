package link.yologram.api.v1.domain.ums.repository

import link.yologram.api.v1.domain.ums.entity.AdminUser
import link.yologram.api.v1.domain.ums.enum.UserStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserRepositoryTest {

    @Autowired
    lateinit var adminUserRepository: AdminUserRepository

    private fun createAdminUser(
        email: String = "admin@yologram.link",
        name: String = "어드민",
        password: String = "encoded-password",
    ) = AdminUser(email = email, name = name, password = password)

    @Nested
    inner class 어드민_저장 {

        @Test
        fun `저장 성공 시 id가 생성된다`() {
            val saved = adminUserRepository.save(createAdminUser())

            assertTrue(saved.id > 0)
        }

        @Test
        fun `저장된 어드민의 필드값이 정확하다`() {
            val saved = adminUserRepository.save(createAdminUser())

            assertEquals("admin@yologram.link", saved.email)
            assertEquals("어드민", saved.name)
            assertEquals(UserStatus.ACTIVE, saved.status)
            assertNotNull(saved.joinedDate)
            assertNotNull(saved.modifiedDate)
        }
    }

    @Nested
    inner class 이메일_조회 {

        @Test
        fun `존재하는 이메일 조회 성공`() {
            adminUserRepository.save(createAdminUser())

            val found = adminUserRepository.findByEmail("admin@yologram.link")

            assertTrue(found.isPresent)
            assertEquals("admin@yologram.link", found.get().email)
        }

        @Test
        fun `존재하지 않는 이메일 조회 시 빈 Optional`() {
            val found = adminUserRepository.findByEmail("notfound@yologram.link")

            assertTrue(found.isEmpty)
        }
    }

    @Nested
    inner class 이메일_중복_확인 {

        @Test
        fun `존재하는 이메일은 true`() {
            adminUserRepository.save(createAdminUser())

            assertTrue(adminUserRepository.existsByEmail("admin@yologram.link"))
        }

        @Test
        fun `존재하지 않는 이메일은 false`() {
            assertFalse(adminUserRepository.existsByEmail("notfound@yologram.link"))
        }
    }

    @Nested
    inner class 이메일_유니크_제약 {

        @Test
        fun `중복 이메일 저장 시 예외 발생`() {
            adminUserRepository.saveAndFlush(createAdminUser(email = "dup@yologram.link"))

            assertThrows(Exception::class.java) {
                adminUserRepository.saveAndFlush(createAdminUser(email = "dup@yologram.link"))
            }
        }
    }
}
