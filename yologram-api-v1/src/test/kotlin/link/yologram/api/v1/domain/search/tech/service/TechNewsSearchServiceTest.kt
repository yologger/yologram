package link.yologram.api.v1.domain.search.tech.service

import link.yologram.api.v1.config.opensearch.OpenSearchProperties
import link.yologram.api.v1.domain.search.exception.BlankSearchKeywordException
import link.yologram.api.v1.domain.search.exception.SearchPageTooDeepException
import link.yologram.api.v1.domain.search.exception.SearchUnavailableException
import link.yologram.api.v1.domain.search.tech.document.TechNewsDocument
import link.yologram.api.v1.domain.search.tech.model.TechSearchSort
import link.yologram.api.v1.domain.search.tech.repository.TechNewsSearchRepository
import link.yologram.api.v1.infra.client.cms.CmsApiClient
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TechNewsSearchServiceTest {

    private val searchRepository = mock<TechNewsSearchRepository>()
    private val cmsApiClient = mock<CmsApiClient>()

    /** 검색 활성 상태 — 비활성이면 503으로 끊기므로 기본은 켜둔다 */
    private val properties = OpenSearchProperties(enabled = true, uri = "https://opensearch.test")

    private val service = TechNewsSearchService(properties, searchRepository, cmsApiClient)

    private fun document(id: Long, categoryIds: List<Long> = listOf(1)) = TechNewsDocument(
        id = id,
        title = "제목 $id",
        summary = "요약 $id",
        link = "https://news.test/$id",
        sourceName = "GeekNews",
        categoryIds = categoryIds,
        publishedAt = LocalDateTime.of(2026, 7, 18, 14, 23, 50),
    )

    private fun givenResult(docs: List<TechNewsDocument>, total: Long) {
        whenever(searchRepository.search(any(), any(), any(), any()))
            .thenReturn(TechNewsSearchRepository.Result(documents = docs, totalCount = total))
    }

    @Nested
    inner class 활성_여부 {

        @Test
        fun `검색 설정이 없으면 503 예외를 던지고 질의하지 않는다`() {
            // 조건부 빈으로 막으면 404가 되어 "없는 경로"로 오해된다 — 게시글 검색과 같이 503으로 알린다
            val disabled = TechNewsSearchService(
                OpenSearchProperties(enabled = false),
                searchRepository,
                cmsApiClient,
            )

            assertFailsWith<SearchUnavailableException> {
                disabled.search("마이그레이션", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)
            }

            verify(searchRepository, never()).search(any(), any(), any(), any())
        }
    }

    @Nested
    inner class 검증 {

        @Test
        fun `검색어가 비면 400 예외를 던지고 질의하지 않는다`() {
            assertFailsWith<BlankSearchKeywordException> {
                service.search("", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)
            }

            verify(searchRepository, never()).search(any(), any(), any(), any())
        }

        @Test
        fun `공백만 있는 검색어도 비어있는 것으로 본다`() {
            assertFailsWith<BlankSearchKeywordException> {
                service.search("   ", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)
            }
        }

        @Test
        fun `검색어의 앞뒤 공백은 잘라서 질의한다`() {
            givenResult(emptyList(), 0)

            service.search("  마이그레이션  ", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)

            verify(searchRepository).search(eq("마이그레이션"), any(), any(), any())
        }

        @Test
        fun `max_result_window를 넘는 페이지는 400 예외를 던진다`() {
            // 막지 않으면 OpenSearch가 예외를 내 500이 된다
            assertFailsWith<SearchPageTooDeepException> {
                service.search("마이그레이션", page = 1000, size = 10, sort = TechSearchSort.RELEVANCE)
            }

            verify(searchRepository, never()).search(any(), any(), any(), any())
        }

        @Test
        fun `한계 직전 페이지는 통과한다`() {
            givenResult(emptyList(), 10_000)

            service.search("마이그레이션", page = 999, size = 10, sort = TechSearchSort.RELEVANCE)

            verify(searchRepository).search(any(), eq(9990), eq(10), any())
        }

        @Test
        fun `size는 1에서 50으로 보정한다`() {
            givenResult(emptyList(), 0)

            service.search("마이그레이션", page = 0, size = 999, sort = TechSearchSort.RELEVANCE)
            verify(searchRepository).search(any(), any(), eq(TechNewsSearchService.MAX_PAGE_SIZE), any())

            service.search("마이그레이션", page = 0, size = 0, sort = TechSearchSort.RELEVANCE)
            verify(searchRepository).search(any(), any(), eq(1), any())
        }

        @Test
        fun `음수 페이지는 0으로 보정한다`() {
            givenResult(emptyList(), 0)

            service.search("마이그레이션", page = -5, size = 10, sort = TechSearchSort.RELEVANCE)

            verify(searchRepository).search(any(), eq(0), any(), any())
        }
    }

    @Nested
    inner class 페이지_계산 {

        @Test
        fun `총건수와 페이지 크기로 전체 페이지 수를 올림 계산한다`() {
            givenResult(listOf(document(1)), 45)

            val result = service.search("마이그레이션", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)

            assertEquals(45, result.totalCount)
            assertEquals(5, result.totalPages)
            assertEquals(0, result.page)
            assertEquals(10, result.size)
        }

        @Test
        fun `첫 페이지는 first true, 마지막 페이지는 last true`() {
            givenResult(listOf(document(1)), 45)

            val first = service.search("마이그레이션", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)
            assertTrue(first.first!!)
            assertFalse(first.last!!)

            val last = service.search("마이그레이션", page = 4, size = 10, sort = TechSearchSort.RELEVANCE)
            assertFalse(last.first!!)
            assertTrue(last.last!!)
        }

        @Test
        fun `결과가 없으면 총 0건이고 첫 페이지가 마지막이다`() {
            givenResult(emptyList(), 0)

            val result = service.search("없는키워드", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)

            assertEquals(0, result.totalCount)
            assertEquals(0, result.totalPages)
            assertTrue(result.data.isEmpty())
            assertTrue(result.first!!)
            assertTrue(result.last!!)
        }

        @Test
        fun `페이지 번호가 from으로 변환된다`() {
            givenResult(emptyList(), 100)

            service.search("마이그레이션", page = 3, size = 20, sort = TechSearchSort.RELEVANCE)

            verify(searchRepository).search(any(), eq(60), eq(20), any())
        }
    }

    @Nested
    inner class 응답_조립 {

        @Test
        fun `색인 문서를 목록 응답 스키마로 변환한다`() {
            givenResult(listOf(document(900)), 1)
            whenever(cmsApiClient.findCategoryNames(any())).thenReturn(mapOf(1L to "인프라"))

            val result = service.search("마이그레이션", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)

            val item = result.data.single()
            assertEquals(900, item.id)
            assertEquals("제목 900", item.title)
            assertEquals("요약 900", item.summary)
            assertEquals("https://news.test/900", item.link)
            assertEquals("GeekNews", item.sourceName)
            assertEquals(listOf("인프라"), item.categories)
            assertEquals(LocalDateTime.of(2026, 7, 18, 14, 23, 50), item.publishedAt)
        }

        @Test
        fun `카테고리 라벨은 id를 모아 한 번만 조회한다`() {
            // 색인에 라벨을 넣지 않기로 했으므로(이름 변경 시 재색인 필요) 조회로 채운다 — N+1이 되면 안 된다
            givenResult(
                listOf(
                    document(1, categoryIds = listOf(1, 2)),
                    document(2, categoryIds = listOf(2)),
                    document(3, categoryIds = listOf(1)),
                ),
                3,
            )
            whenever(cmsApiClient.findCategoryNames(any())).thenReturn(mapOf(1L to "인프라", 2L to "AI"))

            service.search("마이그레이션", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)

            verify(cmsApiClient).findCategoryNames(listOf(1L, 2L))
        }

        @Test
        fun `삭제된 카테고리는 라벨에서 빠진다`() {
            // 매핑은 남아 있는데 마스터에서 사라진 경우 — 목록 API와 같이 조용히 제외한다
            givenResult(listOf(document(1, categoryIds = listOf(1, 99))), 1)
            whenever(cmsApiClient.findCategoryNames(any())).thenReturn(mapOf(1L to "인프라"))

            val result = service.search("마이그레이션", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)

            assertEquals(listOf("인프라"), result.data.single().categories)
        }

        @Test
        fun `결과가 없으면 카테고리 조회도 빈 목록으로 나간다`() {
            givenResult(emptyList(), 0)

            service.search("없는키워드", page = 0, size = 10, sort = TechSearchSort.RELEVANCE)

            verify(cmsApiClient).findCategoryNames(emptyList())
        }
    }
}
