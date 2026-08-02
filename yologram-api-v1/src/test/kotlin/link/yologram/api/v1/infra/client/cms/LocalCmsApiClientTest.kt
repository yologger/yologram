package link.yologram.api.v1.infra.client.cms

import link.yologram.api.v1.domain.cms.tech.entity.TechCategory
import link.yologram.api.v1.domain.cms.tech.repository.TechCategoryRepository
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

@ExtendWith(MockitoExtension::class)
class LocalCmsApiClientTest {

    @Mock
    lateinit var categoryRepository: TechCategoryRepository

    @InjectMocks
    lateinit var client: LocalCmsApiClient

    @Nested
    inner class 활성_검증 {

        @Test
        fun `모두 활성 카테고리면 true`() {
            whenever(categoryRepository.countByIdInAndIsActiveTrue(setOf(1L, 2L))).thenReturn(2L)

            assertTrue(client.allActive(listOf(1L, 2L)))
        }

        @Test
        fun `하나라도 비활성이거나 없으면 false`() {
            whenever(categoryRepository.countByIdInAndIsActiveTrue(setOf(1L, 99L))).thenReturn(1L)

            assertFalse(client.allActive(listOf(1L, 99L)))
        }

        @Test
        fun `빈 목록이면 DB 조회 없이 true`() {
            assertTrue(client.allActive(emptyList()))
            verify(categoryRepository, never()).countByIdInAndIsActiveTrue(any())
        }

        @Test
        fun `중복 id는 distinct로 검증한다`() {
            whenever(categoryRepository.countByIdInAndIsActiveTrue(setOf(1L))).thenReturn(1L)

            assertTrue(client.allActive(listOf(1L, 1L, 1L)))
        }
    }

    @Nested
    inner class 라벨_조회 {

        @Test
        fun `id를 카테고리명으로 매핑해 반환한다`() {
            whenever(categoryRepository.findAllById(setOf(2L, 5L))).thenReturn(
                listOf(
                    TechCategory(id = 2, name = "Backend", sortOrder = 2),
                    TechCategory(id = 5, name = "Cloud", sortOrder = 5),
                )
            )

            val result = client.findCategoryNames(listOf(2L, 5L))

            assertEquals(mapOf(2L to "Backend", 5L to "Cloud"), result)
        }

        @Test
        fun `존재하지 않는(삭제된) id는 결과에서 제외된다`() {
            whenever(categoryRepository.findAllById(setOf(2L, 99L))).thenReturn(
                listOf(TechCategory(id = 2, name = "Backend", sortOrder = 2))
            )

            val result = client.findCategoryNames(listOf(2L, 99L))

            assertEquals(mapOf(2L to "Backend"), result)
        }

        @Test
        fun `빈 목록이면 DB 조회 없이 빈 맵을 반환한다`() {
            assertTrue(client.findCategoryNames(emptyList()).isEmpty())
            verify(categoryRepository, never()).findAllById(any())
        }

        @Test
        fun `중복 id는 한 번만 조회한다`() {
            whenever(categoryRepository.findAllById(setOf(2L))).thenReturn(
                listOf(TechCategory(id = 2, name = "Backend", sortOrder = 2))
            )

            val result = client.findCategoryNames(listOf(2L, 2L))

            assertEquals(mapOf(2L to "Backend"), result)
            verify(categoryRepository).findAllById(setOf(2L))
        }
    }
}
