package link.yologram.api.v1.domain.news.tech.service

import link.yologram.api.v1.domain.news.tech.entity.TechNewsSource
import link.yologram.api.v1.domain.news.tech.exception.TechNewsSourceDuplicateException
import link.yologram.api.v1.domain.news.tech.exception.TechNewsSourceNotFoundException
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceCreateRequest
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceUpdateRequest
import link.yologram.api.v1.domain.news.tech.repository.TechNewsSourceRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminTechNewsSourceServiceTest {

    @Mock
    lateinit var techNewsSourceRepository: TechNewsSourceRepository

    @InjectMocks
    lateinit var adminTechNewsSourceService: AdminTechNewsSourceService

    private fun testSource(
        id: Long = 1L,
        name: String = "GeekNews",
        url: String = "https://news.hada.io/rss/news",
        isActive: Boolean = true,
    ) = TechNewsSource(
        id = id,
        name = name,
        url = url,
        isActive = isActive,
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        modifiedDate = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    @Nested
    inner class 목록_조회 {

        @Test
        fun `전체 소스를 응답 모델로 변환해 반환한다`() {
            val sources = listOf(
                testSource(id = 1L, name = "소스1", url = "https://example.com/rss/1"),
                testSource(id = 2L, name = "소스2", url = "https://example.com/rss/2", isActive = false),
            )
            whenever(techNewsSourceRepository.findAllByOrderByIdAsc()).thenReturn(sources)

            val result = adminTechNewsSourceService.getSources()

            assertEquals(2, result.size)
            assertEquals(1L, result[0].id)
            assertEquals("소스1", result[0].name)
            assertEquals("https://example.com/rss/1", result[0].url)
            assertTrue(result[0].isActive)
            assertEquals(2L, result[1].id)
            assertFalse(result[1].isActive)
        }

        @Test
        fun `소스가 없으면 빈 리스트를 반환한다`() {
            whenever(techNewsSourceRepository.findAllByOrderByIdAsc()).thenReturn(emptyList())

            val result = adminTechNewsSourceService.getSources()

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class 생성 {

        @Test
        fun `생성 성공 시 저장된 소스를 반환한다`() {
            val request = AdminTechNewsSourceCreateRequest(name = "GeekNews", url = "https://news.hada.io/rss/news")
            whenever(techNewsSourceRepository.existsByUrl(request.url)).thenReturn(false)
            whenever(techNewsSourceRepository.save(any<TechNewsSource>())).thenReturn(testSource())

            val result = adminTechNewsSourceService.create(request)

            assertEquals(1L, result.id)
            assertEquals("GeekNews", result.name)
            assertEquals("https://news.hada.io/rss/news", result.url)
            assertTrue(result.isActive)
        }

        @Test
        fun `isActive 생략 시 true로 저장된다`() {
            val request = AdminTechNewsSourceCreateRequest(name = "GeekNews", url = "https://news.hada.io/rss/news")
            whenever(techNewsSourceRepository.existsByUrl(request.url)).thenReturn(false)
            whenever(techNewsSourceRepository.save(any<TechNewsSource>())).thenAnswer {
                val source = it.arguments[0] as TechNewsSource
                assertTrue(source.isActive)
                source
            }

            adminTechNewsSourceService.create(request)
        }

        @Test
        fun `isActive false를 지정해 저장할 수 있다`() {
            val request = AdminTechNewsSourceCreateRequest(
                name = "GeekNews",
                url = "https://news.hada.io/rss/news",
                isActive = false,
            )
            whenever(techNewsSourceRepository.existsByUrl(request.url)).thenReturn(false)
            whenever(techNewsSourceRepository.save(any<TechNewsSource>())).thenAnswer {
                val source = it.arguments[0] as TechNewsSource
                assertFalse(source.isActive)
                source
            }

            adminTechNewsSourceService.create(request)
        }

        @Test
        fun `url 중복 시 TechNewsSourceDuplicateException 발생`() {
            val request = AdminTechNewsSourceCreateRequest(name = "GeekNews", url = "https://news.hada.io/rss/news")
            whenever(techNewsSourceRepository.existsByUrl(request.url)).thenReturn(true)

            val exception = assertThrows<TechNewsSourceDuplicateException> {
                adminTechNewsSourceService.create(request)
            }

            assertEquals("NEWS_SOURCE_DUPLICATE", exception.errorCode)
            verify(techNewsSourceRepository, never()).save(any<TechNewsSource>())
        }
    }

    @Nested
    inner class 수정 {

        @Test
        fun `name만 보내면 name만 변경된다`() {
            val source = testSource()
            whenever(techNewsSourceRepository.findById(1L)).thenReturn(Optional.of(source))
            whenever(techNewsSourceRepository.saveAndFlush(any<TechNewsSource>())).thenAnswer { it.arguments[0] }

            val result = adminTechNewsSourceService.update(1L, AdminTechNewsSourceUpdateRequest(name = "새이름"))

            assertEquals("새이름", result.name)
            assertEquals("https://news.hada.io/rss/news", result.url)
            assertTrue(result.isActive)
        }

        @Test
        fun `url만 보내면 url만 변경된다`() {
            val source = testSource()
            whenever(techNewsSourceRepository.findById(1L)).thenReturn(Optional.of(source))
            whenever(techNewsSourceRepository.existsByUrlAndIdNot("https://example.com/rss/new", 1L)).thenReturn(false)
            whenever(techNewsSourceRepository.saveAndFlush(any<TechNewsSource>())).thenAnswer { it.arguments[0] }

            val result = adminTechNewsSourceService.update(
                1L,
                AdminTechNewsSourceUpdateRequest(url = "https://example.com/rss/new"),
            )

            assertEquals("GeekNews", result.name)
            assertEquals("https://example.com/rss/new", result.url)
        }

        @Test
        fun `isActive만 보내면 isActive만 변경된다`() {
            val source = testSource()
            whenever(techNewsSourceRepository.findById(1L)).thenReturn(Optional.of(source))
            whenever(techNewsSourceRepository.saveAndFlush(any<TechNewsSource>())).thenAnswer { it.arguments[0] }

            val result = adminTechNewsSourceService.update(1L, AdminTechNewsSourceUpdateRequest(isActive = false))

            assertEquals("GeekNews", result.name)
            assertEquals("https://news.hada.io/rss/news", result.url)
            assertFalse(result.isActive)
        }

        @Test
        fun `모든 필드를 보내면 전부 변경된다`() {
            val source = testSource()
            whenever(techNewsSourceRepository.findById(1L)).thenReturn(Optional.of(source))
            whenever(techNewsSourceRepository.existsByUrlAndIdNot("https://example.com/rss/new", 1L)).thenReturn(false)
            whenever(techNewsSourceRepository.saveAndFlush(any<TechNewsSource>())).thenAnswer { it.arguments[0] }

            val result = adminTechNewsSourceService.update(
                1L,
                AdminTechNewsSourceUpdateRequest(name = "새이름", url = "https://example.com/rss/new", isActive = false),
            )

            assertEquals("새이름", result.name)
            assertEquals("https://example.com/rss/new", result.url)
            assertFalse(result.isActive)
        }

        @Test
        fun `모든 필드가 널이면 아무것도 변경되지 않는다`() {
            val source = testSource()
            whenever(techNewsSourceRepository.findById(1L)).thenReturn(Optional.of(source))
            whenever(techNewsSourceRepository.saveAndFlush(any<TechNewsSource>())).thenAnswer { it.arguments[0] }

            val result = adminTechNewsSourceService.update(1L, AdminTechNewsSourceUpdateRequest())

            assertEquals("GeekNews", result.name)
            assertEquals("https://news.hada.io/rss/news", result.url)
            assertTrue(result.isActive)
        }

        @Test
        fun `없는 id면 TechNewsSourceNotFoundException 발생`() {
            whenever(techNewsSourceRepository.findById(999L)).thenReturn(Optional.empty())

            val exception = assertThrows<TechNewsSourceNotFoundException> {
                adminTechNewsSourceService.update(999L, AdminTechNewsSourceUpdateRequest(name = "새이름"))
            }

            assertEquals("NEWS_SOURCE_NOT_FOUND", exception.errorCode)
        }

        @Test
        fun `다른 소스와 url이 중복되면 TechNewsSourceDuplicateException 발생`() {
            val source = testSource()
            whenever(techNewsSourceRepository.findById(1L)).thenReturn(Optional.of(source))
            whenever(techNewsSourceRepository.existsByUrlAndIdNot("https://example.com/rss/dup", 1L)).thenReturn(true)

            val exception = assertThrows<TechNewsSourceDuplicateException> {
                adminTechNewsSourceService.update(1L, AdminTechNewsSourceUpdateRequest(url = "https://example.com/rss/dup"))
            }

            assertEquals("NEWS_SOURCE_DUPLICATE", exception.errorCode)
            verify(techNewsSourceRepository, never()).saveAndFlush(any<TechNewsSource>())
        }

        @Test
        fun `자기 자신의 url 그대로 보내면 중복이 아니다`() {
            val source = testSource()
            whenever(techNewsSourceRepository.findById(1L)).thenReturn(Optional.of(source))
            whenever(techNewsSourceRepository.existsByUrlAndIdNot(source.url, 1L)).thenReturn(false)
            whenever(techNewsSourceRepository.saveAndFlush(any<TechNewsSource>())).thenAnswer { it.arguments[0] }

            val result = adminTechNewsSourceService.update(1L, AdminTechNewsSourceUpdateRequest(url = source.url))

            assertEquals("https://news.hada.io/rss/news", result.url)
        }
    }

    @Nested
    inner class 삭제 {

        @Test
        fun `삭제 성공 시 repository delete가 호출된다`() {
            val source = testSource()
            whenever(techNewsSourceRepository.findById(1L)).thenReturn(Optional.of(source))

            adminTechNewsSourceService.delete(1L)

            verify(techNewsSourceRepository).delete(source)
        }

        @Test
        fun `없는 id면 TechNewsSourceNotFoundException 발생`() {
            whenever(techNewsSourceRepository.findById(999L)).thenReturn(Optional.empty())

            val exception = assertThrows<TechNewsSourceNotFoundException> {
                adminTechNewsSourceService.delete(999L)
            }

            assertEquals("NEWS_SOURCE_NOT_FOUND", exception.errorCode)
            verify(techNewsSourceRepository, never()).delete(any<TechNewsSource>())
        }
    }
}
