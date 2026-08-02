package link.yologram.worker.infra.client.cms

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalCmsApiClientTest {

    @Test
    fun `활성 카테고리를 sortOrder 순으로 그대로 반환한다`() {
        val categories = listOf(
            TechCategory(id = 1, name = "Frontend", sortOrder = 1),
            TechCategory(id = 2, name = "Backend", sortOrder = 2),
            TechCategory(id = 7, name = "기타", sortOrder = 7),
        )
        val repository: TechCategoryRepository = mock {
            on { findByIsActiveTrueOrderBySortOrder() } doReturn categories
        }
        val client = LocalCmsApiClient(repository)

        val result = client.findActiveCategories()

        assertEquals(categories, result)
        verify(repository).findByIsActiveTrueOrderBySortOrder()
    }

    @Test
    fun `활성 카테고리가 없으면 빈 목록을 반환한다`() {
        val repository: TechCategoryRepository = mock {
            on { findByIsActiveTrueOrderBySortOrder() } doReturn emptyList()
        }
        val client = LocalCmsApiClient(repository)

        assertTrue(client.findActiveCategories().isEmpty())
    }
}
