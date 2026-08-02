package link.yologram.api.v1.infra.client.ums

import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LocalUmsApiClientTest {

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var client: LocalUmsApiClient

    private fun user(id: Long, nickname: String) =
        User(id = id, email = "u$id@yologram.link", name = "이름$id", nickname = nickname, password = "encoded")

    @Nested
    inner class 단건_조회 {

        @Test
        fun `존재하는 uid면 닉네임을 반환한다`() {
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "nick1")))

            assertEquals("nick1", client.findNickname(1L))
        }

        @Test
        fun `없는 uid면 null을 반환한다`() {
            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            assertNull(client.findNickname(999L))
        }
    }

    @Nested
    inner class 배치_조회 {

        @Test
        fun `uid별 닉네임 맵을 반환하고 없는 uid는 제외한다`() {
            whenever(userRepository.findAllById(setOf(1L, 2L, 999L)))
                .thenReturn(listOf(user(1L, "nick1"), user(2L, "nick2")))

            val result = client.findNicknames(listOf(1L, 2L, 999L))

            assertEquals(mapOf(1L to "nick1", 2L to "nick2"), result)
        }

        @Test
        fun `중복 uid는 집합으로 정리해 조회한다`() {
            whenever(userRepository.findAllById(setOf(1L))).thenReturn(listOf(user(1L, "nick1")))

            val result = client.findNicknames(listOf(1L, 1L, 1L))

            assertEquals(mapOf(1L to "nick1"), result)
            verify(userRepository).findAllById(setOf(1L))
        }

        @Test
        fun `빈 입력이면 DB를 호출하지 않는다`() {
            val result = client.findNicknames(emptyList())

            assertTrue(result.isEmpty())
            verify(userRepository, never()).findAllById(any())
        }
    }
}
