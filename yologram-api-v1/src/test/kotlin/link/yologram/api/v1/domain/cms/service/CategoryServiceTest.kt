package link.yologram.api.v1.domain.cms.service

import link.yologram.api.v1.domain.cms.entity.Category
import link.yologram.api.v1.domain.cms.enum.Section
import link.yologram.api.v1.domain.cms.exception.InvalidSectionException
import link.yologram.api.v1.domain.cms.repository.CategoryRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class CategoryServiceTest {

    @Mock
    lateinit var categoryRepository: CategoryRepository

    @InjectMocks
    lateinit var categoryService: CategoryService

    private fun category(id: Long, name: String, sortOrder: Int, section: Section = Section.TECH) =
        Category(id = id, section = section, name = name, sortOrder = sortOrder)

    @Nested
    inner class 카테고리_조회 {

        @Test
        fun `section별 활성 카테고리를 정렬 순으로 반환한다`() {
            whenever(categoryRepository.findBySectionAndIsActiveTrueOrderBySortOrderAsc(Section.TECH))
                .thenReturn(
                    listOf(
                        category(1L, "Frontend", 1),
                        category(2L, "Backend", 2),
                    )
                )

            val result = categoryService.getCategories("tech")

            assertEquals(2, result.size)
            assertEquals(1L, result[0].id)
            assertEquals("Frontend", result[0].name)
            assertEquals(1, result[0].sortOrder)
            assertEquals("Backend", result[1].name)
        }

        @Test
        fun `대문자 section path도 허용한다`() {
            whenever(categoryRepository.findBySectionAndIsActiveTrueOrderBySortOrderAsc(Section.INVEST))
                .thenReturn(listOf(category(10L, "국내주식", 1, Section.INVEST)))

            val result = categoryService.getCategories("INVEST")

            assertEquals(1, result.size)
            assertEquals("국내주식", result[0].name)
        }

        @Test
        fun `카테고리가 없으면 빈 목록을 반환한다`() {
            whenever(categoryRepository.findBySectionAndIsActiveTrueOrderBySortOrderAsc(Section.POLITICS))
                .thenReturn(emptyList())

            val result = categoryService.getCategories("politics")

            assertTrue(result.isEmpty())
        }

        @Test
        fun `유효하지 않은 section이면 InvalidSectionException을 던진다`() {
            assertThrows<InvalidSectionException> {
                categoryService.getCategories("unknown")
            }

            verify(categoryRepository, never()).findBySectionAndIsActiveTrueOrderBySortOrderAsc(org.mockito.kotlin.any())
        }
    }
}
