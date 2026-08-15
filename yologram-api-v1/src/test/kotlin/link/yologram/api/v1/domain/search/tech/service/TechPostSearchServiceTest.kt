package link.yologram.api.v1.domain.search.tech.service

import link.yologram.api.v1.config.opensearch.OpenSearchProperties
import link.yologram.api.v1.domain.pms.tech.entity.TechPostLike
import link.yologram.api.v1.domain.pms.tech.repository.TechPostLikeRepository
import link.yologram.api.v1.domain.search.exception.BlankSearchKeywordException
import link.yologram.api.v1.domain.search.exception.SearchPageTooDeepException
import link.yologram.api.v1.domain.search.exception.SearchUnavailableException
import link.yologram.api.v1.domain.search.tech.document.TechPostDocument
import link.yologram.api.v1.domain.search.tech.model.TechSearchSort
import link.yologram.api.v1.domain.search.tech.repository.TechPostSearchRepository
import link.yologram.api.v1.infra.client.ums.UmsApiClient
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

class TechPostSearchServiceTest {

    private val searchRepository = mock<TechPostSearchRepository>()
    private val umsApiClient = mock<UmsApiClient>()
    private val likeRepository = mock<TechPostLikeRepository>()

    /** 검색 활성 상태 — 비활성이면 503으로 끊기므로 기본은 켜둔다 */
    private val properties = OpenSearchProperties(enabled = true, uri = "https://opensearch.test")

    private val service = TechPostSearchService(properties, searchRepository, umsApiClient, likeRepository)

    private fun document(id: Long, uid: Long = 12) = TechPostDocument(
        id = id,
        uid = uid,
        title = "제목 $id",
        content = "본문 $id",
        categoryIds = listOf(1),
        metrics = TechPostDocument.Metrics(commentCount = 2, likeCount = 3, viewCount = 4),
        createdAt = LocalDateTime.of(2026, 7, 18, 14, 23, 50),
    )

    private fun givenResult(docs: List<TechPostDocument>, total: Long) {
        whenever(searchRepository.search(any(), any(), any(), any()))
            .thenReturn(TechPostSearchRepository.Result(documents = docs, totalCount = total))
    }

    @Nested
    inner class 활성_여부 {

        @Test
        fun `검색 설정이 없으면 503 예외를 던지고 질의하지 않는다`() {
            // 조건부 빈으로 막으면 404가 되어 "없는 경로"로 오해된다 — api-v2와 같이 503으로 알린다
            val disabled = TechPostSearchService(
                OpenSearchProperties(enabled = false),
                searchRepository,
                umsApiClient,
                likeRepository,
            )

            assertFailsWith<SearchUnavailableException> {
                disabled.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)
            }

            verify(searchRepository, never()).search(any(), any(), any(), any())
        }
    }

    @Nested
    inner class 검증 {

        @Test
        fun `검색어가 비면 400 예외를 던지고 질의하지 않는다`() {
            assertFailsWith<BlankSearchKeywordException> {
                service.search("", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)
            }

            verify(searchRepository, never()).search(any(), any(), any(), any())
        }

        @Test
        fun `공백만 있는 검색어도 비어있는 것으로 본다`() {
            assertFailsWith<BlankSearchKeywordException> {
                service.search("   ", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)
            }
        }

        @Test
        fun `검색어의 앞뒤 공백은 잘라서 질의한다`() {
            givenResult(emptyList(), 0)

            service.search("  제미나이  ", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            verify(searchRepository).search(eq("제미나이"), any(), any(), any())
        }

        @Test
        fun `max_result_window를 넘는 페이지는 400 예외를 던진다`() {
            // 막지 않으면 OpenSearch가 예외를 내 500이 된다
            assertFailsWith<SearchPageTooDeepException> {
                service.search("제미나이", page = 1000, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)
            }

            verify(searchRepository, never()).search(any(), any(), any(), any())
        }

        @Test
        fun `한계 직전 페이지는 통과한다`() {
            givenResult(emptyList(), 10_000)

            service.search("제미나이", page = 999, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            verify(searchRepository).search(any(), eq(9990), eq(10), any())
        }

        @Test
        fun `size는 1에서 50으로 보정한다`() {
            givenResult(emptyList(), 0)

            service.search("제미나이", page = 0, size = 999, sort = TechSearchSort.RELEVANCE, viewerUid = null)
            verify(searchRepository).search(any(), any(), eq(TechPostSearchService.MAX_PAGE_SIZE), any())

            service.search("제미나이", page = 0, size = 0, sort = TechSearchSort.RELEVANCE, viewerUid = null)
            verify(searchRepository).search(any(), any(), eq(1), any())
        }

        @Test
        fun `음수 페이지는 0으로 보정한다`() {
            givenResult(emptyList(), 0)

            service.search("제미나이", page = -5, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            verify(searchRepository).search(any(), eq(0), any(), any())
        }
    }

    @Nested
    inner class 페이지_계산 {

        @Test
        fun `총건수와 페이지 크기로 전체 페이지 수를 올림 계산한다`() {
            givenResult(listOf(document(1)), 45)

            val result = service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            assertEquals(45, result.totalCount)
            assertEquals(5, result.totalPages)
            assertEquals(0, result.page)
            assertEquals(10, result.size)
        }

        @Test
        fun `첫 페이지는 first true, 마지막 페이지는 last true`() {
            givenResult(listOf(document(1)), 45)

            val first = service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)
            assertTrue(first.first!!)
            assertFalse(first.last!!)

            val last = service.search("제미나이", page = 4, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)
            assertFalse(last.first!!)
            assertTrue(last.last!!)
        }

        @Test
        fun `결과가 없으면 총 0건이고 첫 페이지가 마지막이다`() {
            givenResult(emptyList(), 0)

            val result = service.search("없는키워드", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            assertEquals(0, result.totalCount)
            assertEquals(0, result.totalPages)
            assertTrue(result.data.isEmpty())
            assertTrue(result.first!!)
            assertTrue(result.last!!)
        }

        @Test
        fun `페이지 번호가 from으로 변환된다`() {
            givenResult(emptyList(), 100)

            service.search("제미나이", page = 3, size = 20, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            verify(searchRepository).search(any(), eq(60), eq(20), any())
        }
    }

    @Nested
    inner class 응답_조립 {

        @Test
        fun `색인 문서를 목록 응답 스키마로 변환한다`() {
            givenResult(listOf(document(1200)), 1)
            whenever(umsApiClient.findNicknames(any())).thenReturn(mapOf(12L to "tester0"))

            val result = service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            val item = result.data.single()
            assertEquals(1200, item.id)
            assertEquals("TECH", item.section)
            assertEquals(12, item.author.uid)
            assertEquals("tester0", item.author.nickname)
            assertEquals("제목 1200", item.title)
            assertEquals(listOf(1L), item.categoryIds)
            assertEquals(2, item.metrics.commentCount)
            assertEquals(3, item.metrics.likeCount)
            assertEquals(4, item.metrics.viewCount)
            assertEquals(LocalDateTime.of(2026, 7, 18, 14, 23, 50), item.createdAt)
        }

        @Test
        fun `닉네임은 uid를 모아 한 번만 조회한다`() {
            // 색인에 닉네임을 넣지 않기로 했으므로(변경 시 재색인 필요) 조회로 채운다 — N+1이 되면 안 된다
            givenResult(listOf(document(1, uid = 12), document(2, uid = 13), document(3, uid = 12)), 3)
            whenever(umsApiClient.findNicknames(any())).thenReturn(mapOf(12L to "a", 13L to "b"))

            service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            verify(umsApiClient).findNicknames(eq(listOf(12L, 13L, 12L)))
        }

        @Test
        fun `닉네임이 없으면 null로 둔다`() {
            givenResult(listOf(document(1)), 1)
            whenever(umsApiClient.findNicknames(any())).thenReturn(emptyMap())

            val result = service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            assertEquals(null, result.data.single().author.nickname)
        }
    }

    @Nested
    inner class 개인화 {

        @Test
        fun `비로그인은 likedByMe가 false이고 이력을 조회하지 않는다`() {
            givenResult(listOf(document(1)), 1)
            whenever(umsApiClient.findNicknames(any())).thenReturn(emptyMap())

            val result = service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = null)

            assertFalse(result.data.single().metrics.likedByMe)
            verify(likeRepository, never()).findByUidAndPostIdIn(any(), any())
        }

        @Test
        fun `로그인 유저가 누른 글만 likedByMe가 true다`() {
            givenResult(listOf(document(1), document(2)), 2)
            whenever(umsApiClient.findNicknames(any())).thenReturn(emptyMap())
            whenever(likeRepository.findByUidAndPostIdIn(eq(12L), any()))
                .thenReturn(listOf(TechPostLike(postId = 1, uid = 12)))

            val result = service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = 12)

            assertTrue(result.data.first { it.id == 1L }.metrics.likedByMe)
            assertFalse(result.data.first { it.id == 2L }.metrics.likedByMe)
        }

        @Test
        fun `결과가 비면 이력을 조회하지 않는다`() {
            givenResult(emptyList(), 0)

            service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.RELEVANCE, viewerUid = 12)

            verify(likeRepository, never()).findByUidAndPostIdIn(any(), any())
        }
    }

    @Nested
    inner class 정렬 {

        @Test
        fun `정렬 기준을 그대로 리포지토리에 전달한다`() {
            givenResult(emptyList(), 0)

            service.search("제미나이", page = 0, size = 10, sort = TechSearchSort.LATEST, viewerUid = null)

            verify(searchRepository).search(any(), any(), any(), eq(TechSearchSort.LATEST))
        }
    }
}
