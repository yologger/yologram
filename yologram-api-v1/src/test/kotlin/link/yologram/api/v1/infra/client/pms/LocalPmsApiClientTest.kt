package link.yologram.api.v1.infra.client.pms

import link.yologram.api.v1.domain.pms.tech.repository.TechPostCommentCountRepository
import link.yologram.api.v1.domain.pms.tech.repository.TechPostRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
class LocalPmsApiClientTest {

    @Mock
    lateinit var postRepository: TechPostRepository

    @Mock
    lateinit var postCommentCountRepository: TechPostCommentCountRepository

    @InjectMocks
    lateinit var client: LocalPmsApiClient

    @Nested
    inner class 존재_검증 {

        @Test
        fun `게시글이 있으면 true`() {
            whenever(postRepository.existsById(1L)).thenReturn(true)

            assertTrue(client.exists(1L))
        }

        @Test
        fun `게시글이 없으면 false`() {
            whenever(postRepository.existsById(99L)).thenReturn(false)

            assertFalse(client.exists(99L))
        }
    }

    @Nested
    inner class 댓글_수_갱신 {

        @Test
        fun `increasePostCommentCount는 카운트 리포지토리 increase로 위임한다`() {
            client.increasePostCommentCount(100L)

            verify(postCommentCountRepository).increase(100L)
            verify(postCommentCountRepository, never()).decrease(any())
        }

        @Test
        fun `decreasePostCommentCount는 카운트 리포지토리 decrease로 위임한다`() {
            client.decreasePostCommentCount(100L)

            verify(postCommentCountRepository).decrease(100L)
            verify(postCommentCountRepository, never()).increase(any())
        }
    }
}
