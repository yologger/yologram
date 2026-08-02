package link.yologram.api.v1.infra.client.ums

import link.yologram.api.v1.domain.ums.entity.User
import link.yologram.api.v1.domain.ums.repository.UserRepository
import link.yologram.api.v1.infra.cache.Cache
import link.yologram.api.v1.infra.cache.CacheService
import link.yologram.api.v1.infra.cache.UserNicknameCache
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LocalUmsApiClientTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var cacheService: CacheService

    lateinit var client: LocalUmsApiClient

    @BeforeEach
    fun setUp() {
        client = LocalUmsApiClient(userRepository, UserNicknameCache(cacheService))
    }

    private fun user(id: Long, nickname: String) = User(
        id = id,
        email = "user$id@yologram.link",
        name = "유저$id",
        nickname = nickname,
        password = "encoded-password",
    )

    private fun key(uid: Long) = Cache.userNickname(uid).key

    @Nested
    inner class 배치_조회 {

        @Test
        fun `전체 캐시 히트 시 DB를 호출하지 않는다`() {
            whenever(cacheService.getAllAsMap(any<List<Cache<String>>>()))
                .thenReturn(mapOf(key(1L) to "닉1", key(2L) to "닉2"))

            val result = client.findNicknames(listOf(1L, 2L))

            assertEquals(mapOf(1L to "닉1", 2L to "닉2"), result)
            verify(userRepository, never()).findAllById(any())
            verify(cacheService, never()).setAll(any<Map<Cache<String>, String>>())
        }

        @Test
        fun `부분 미스 시 미스 uid만 IN 조회하고 캐시를 채운다`() {
            whenever(cacheService.getAllAsMap(any<List<Cache<String>>>()))
                .thenReturn(mapOf(key(1L) to "닉1"))
            whenever(userRepository.findAllById(setOf(2L, 3L)))
                .thenReturn(listOf(user(2L, "닉2"), user(3L, "닉3")))

            val result = client.findNicknames(listOf(1L, 2L, 3L))

            assertEquals(mapOf(1L to "닉1", 2L to "닉2", 3L to "닉3"), result)
            verify(userRepository).findAllById(setOf(2L, 3L))

            // Cache는 TypeReference를 담아 인스턴스 동등성 비교가 안 되므로 key로 검증
            val captor = argumentCaptor<Map<Cache<String>, String>>()
            verify(cacheService).setAll(captor.capture())
            assertEquals(
                mapOf(key(2L) to "닉2", key(3L) to "닉3"),
                captor.firstValue.mapKeys { it.key.key },
            )
        }

        @Test
        fun `전체 미스(Redis 실패 포함) 시 전체를 DB에서 조회한다`() {
            // RedisCacheService는 장애 시 빈 맵을 반환 — 전체 미스와 동일 경로로 DB 폴백돼 기능 무손상
            whenever(cacheService.getAllAsMap(any<List<Cache<String>>>())).thenReturn(emptyMap())
            whenever(userRepository.findAllById(setOf(1L, 2L)))
                .thenReturn(listOf(user(1L, "닉1"), user(2L, "닉2")))

            val result = client.findNicknames(listOf(1L, 2L))

            assertEquals(mapOf(1L to "닉1", 2L to "닉2"), result)
            verify(userRepository).findAllById(setOf(1L, 2L))
        }

        @Test
        fun `존재하지 않는 uid는 결과에서 제외되고 캐시에도 넣지 않는다`() {
            whenever(cacheService.getAllAsMap(any<List<Cache<String>>>())).thenReturn(emptyMap())
            whenever(userRepository.findAllById(setOf(1L, 999L))).thenReturn(listOf(user(1L, "닉1")))

            val result = client.findNicknames(listOf(1L, 999L))

            assertEquals(mapOf(1L to "닉1"), result)
            val captor = argumentCaptor<Map<Cache<String>, String>>()
            verify(cacheService).setAll(captor.capture())
            assertEquals(mapOf(key(1L) to "닉1"), captor.firstValue.mapKeys { it.key.key })
        }

        @Test
        fun `DB에 아무도 없으면 캐시를 채우지 않는다`() {
            whenever(cacheService.getAllAsMap(any<List<Cache<String>>>())).thenReturn(emptyMap())
            whenever(userRepository.findAllById(setOf(999L))).thenReturn(emptyList())

            val result = client.findNicknames(listOf(999L))

            assertTrue(result.isEmpty())
            verify(cacheService, never()).setAll(any<Map<Cache<String>, String>>())
        }

        @Test
        fun `중복 uid는 한 번만 조회한다`() {
            whenever(cacheService.getAllAsMap(any<List<Cache<String>>>())).thenReturn(emptyMap())
            whenever(userRepository.findAllById(setOf(1L))).thenReturn(listOf(user(1L, "닉1")))

            val result = client.findNicknames(listOf(1L, 1L, 1L))

            assertEquals(mapOf(1L to "닉1"), result)
            verify(userRepository).findAllById(setOf(1L))
        }

        @Test
        fun `빈 입력이면 캐시도 DB도 호출하지 않는다`() {
            val result = client.findNicknames(emptyList())

            assertTrue(result.isEmpty())
            verifyNoInteractions(cacheService, userRepository)
        }
    }

    @Nested
    inner class 단건_조회 {

        @Test
        fun `캐시 히트 시 DB를 호출하지 않는다`() {
            whenever(cacheService.getOrNull(argThat<Cache<String>> { key == key(1L) })).thenReturn("닉1")

            val result = client.findNickname(1L)

            assertEquals("닉1", result)
            verify(userRepository, never()).findById(any())
        }

        @Test
        fun `캐시 미스 시 DB에서 조회하고 캐시를 채운다`() {
            whenever(cacheService.getOrNull(any<Cache<String>>())).thenReturn(null)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "닉1")))

            val result = client.findNickname(1L)

            assertEquals("닉1", result)
            verify(cacheService).set(argThat<Cache<String>> { key == key(1L) }, eq("닉1"))
        }

        @Test
        fun `존재하지 않는 uid면 null을 반환하고 캐시에 넣지 않는다`() {
            whenever(cacheService.getOrNull(any<Cache<String>>())).thenReturn(null)
            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            val result = client.findNickname(999L)

            assertNull(result)
            verify(cacheService, never()).set(any<Cache<String>>(), any())
        }
    }
}
