package link.yologram.api.v1.domain.tech.category.service

import link.yologram.api.v1.domain.tech.category.entity.TechCategory
import link.yologram.api.v1.domain.tech.category.repository.TechCategoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class TechCategoryServiceTest {

    @Mock
    lateinit var categoryRepository: TechCategoryRepository

    @InjectMocks
    lateinit var categoryService: TechCategoryService

    private fun category(id: Long, name: String, sortOrder: Int) =
        TechCategory(id = id, name = name, sortOrder = sortOrder)

    @Nested
    inner class 카테고리_조회 {

        @Test
        fun `활성 카테고리를 정렬 순으로 반환한다`() {
            whenever(categoryRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(
                    listOf(
                        category(1L, "Frontend", 1),
                        category(2L, "Backend", 2),
                    )
                )

            val result = categoryService.getCategories()

            assertEquals(2, result.size)
            assertEquals(1L, result[0].id)
            assertEquals("Frontend", result[0].name)
            assertEquals(1, result[0].sortOrder)
            assertEquals("Backend", result[1].name)
        }

        @Test
        fun `카테고리가 없으면 빈 목록을 반환한다`() {
            whenever(categoryRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(emptyList())

            val result = categoryService.getCategories()

            assertTrue(result.isEmpty())
        }
    }
}
