package link.yologram.api.v1.domain.ums.repository

import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.enum.UserStatus
import link.yologram.api.v1.domain.ums.enum.UserType
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
class UserRepositoryTest {

    @Autowired
    lateinit var userRepository: UserRepository

    private fun createUser(
        email: String = "test@yologram.link",
        name: String = "테스터",
        nickname: String = "tester",
        password: String = "encoded-password",
    ) = User(email = email, name = name, nickname = nickname, password = password)

    @Nested
    inner class 유저_저장 {

        @Test
        fun `저장 성공 시 id가 생성된다`() {
            val saved = userRepository.save(createUser())

            assertTrue(saved.id > 0)
        }

        @Test
        fun `저장된 유저의 필드값이 정확하다`() {
            val saved = userRepository.save(createUser())

            assertEquals("test@yologram.link", saved.email)
            assertEquals("테스터", saved.name)
            assertEquals("tester", saved.nickname)
            assertEquals(UserType.DEFAULT, saved.type)
            assertEquals(UserStatus.ACTIVE, saved.status)
            assertNotNull(saved.joinedDate)
            assertNotNull(saved.modifiedDate)
        }
    }

    @Nested
    inner class 이메일_조회 {

        @Test
        fun `존재하는 이메일 조회 성공`() {
            userRepository.save(createUser())

            val found = userRepository.findByEmail("test@yologram.link")

            assertTrue(found.isPresent)
            assertEquals("test@yologram.link", found.get().email)
        }

        @Test
        fun `존재하지 않는 이메일 조회 시 빈 Optional`() {
            val found = userRepository.findByEmail("notfound@yologram.link")

            assertTrue(found.isEmpty)
        }
    }

    @Nested
    inner class 이메일_중복_확인 {

        @Test
        fun `존재하는 이메일은 true`() {
            userRepository.save(createUser())

            assertTrue(userRepository.existsByEmail("test@yologram.link"))
        }

        @Test
        fun `존재하지 않는 이메일은 false`() {
            assertFalse(userRepository.existsByEmail("notfound@yologram.link"))
        }
    }

    @Nested
    inner class 이메일_유니크_제약 {

        @Test
        fun `중복 이메일 저장 시 예외 발생`() {
            userRepository.saveAndFlush(createUser(email = "dup@yologram.link"))

            assertThrows(Exception::class.java) {
                userRepository.saveAndFlush(createUser(email = "dup@yologram.link"))
            }
        }
    }
}
