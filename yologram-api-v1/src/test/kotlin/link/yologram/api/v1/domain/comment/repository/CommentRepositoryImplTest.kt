package link.yologram.api.v1.domain.comment.repository

import jakarta.persistence.EntityManager
import link.yologram.api.v1.domain.comment.entity.Comment
import link.yologram.api.v1.domain.comment.model.CommentSort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentRepositoryImplTest {

    @Autowired
    lateinit var commentRepository: CommentRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        commentRepository.deleteAll()
        entityManager.flush()
    }

    private fun saveComment(postId: Long, userId: Long = 1L): Comment =
        commentRepository.save(Comment(postId = postId, userId = userId, content = "내용"))

    @Nested
    inner class 글_필터 {

        @Test
        fun `요청한 postId의 댓글만 반환한다`() {
            saveComment(postId = 100L)
            saveComment(postId = 200L)
            saveComment(postId = 100L)

            val result = commentRepository.findByPost(100L, CommentSort.LATEST, null, 10)

            assertEquals(2, result.size)
            assertTrue(result.all { it.postId == 100L })
        }
    }

    @Nested
    inner class 정렬 {

        @Test
        fun `최신순은 id 내림차순으로 정렬한다`() {
            val a = saveComment(postId = 100L)
            val b = saveComment(postId = 100L)
            val c = saveComment(postId = 100L)

            val result = commentRepository.findByPost(100L, CommentSort.LATEST, null, 10)

            assertEquals(listOf(c.id, b.id, a.id), result.map { it.id })
        }

        @Test
        fun `오래된순은 id 오름차순으로 정렬한다`() {
            val a = saveComment(postId = 100L)
            val b = saveComment(postId = 100L)
            val c = saveComment(postId = 100L)

            val result = commentRepository.findByPost(100L, CommentSort.OLDEST, null, 10)

            assertEquals(listOf(a.id, b.id, c.id), result.map { it.id })
        }
    }

    @Nested
    inner class 커서 {

        @Test
        fun `최신순 커서는 cursorId보다 작은(과거) 댓글만 반환한다`() {
            val a = saveComment(postId = 100L)
            val b = saveComment(postId = 100L)
            saveComment(postId = 100L)

            // cursorId를 Long?로 명시 — 그러지 않으면 Kotlin이 더 구체적인 offset(Long) 오버로드를 택한다
            val cursorId: Long? = b.id
            val result = commentRepository.findByPost(100L, CommentSort.LATEST, cursorId, 10)

            // b보다 과거인 a만
            assertEquals(listOf(a.id), result.map { it.id })
        }

        @Test
        fun `오래된순 커서는 cursorId보다 큰(이후) 댓글만 반환한다`() {
            saveComment(postId = 100L)
            val b = saveComment(postId = 100L)
            val c = saveComment(postId = 100L)

            // cursorId를 Long?로 명시 — 그러지 않으면 Kotlin이 더 구체적인 offset(Long) 오버로드를 택한다
            val cursorId: Long? = b.id
            val result = commentRepository.findByPost(100L, CommentSort.OLDEST, cursorId, 10)

            // b보다 이후인 c만
            assertEquals(listOf(c.id), result.map { it.id })
        }
    }

    @Nested
    inner class 개수_제한 {

        @Test
        fun `limit 개수만큼만 반환한다`() {
            saveComment(postId = 100L)
            saveComment(postId = 100L)
            saveComment(postId = 100L)

            val result = commentRepository.findByPost(100L, CommentSort.LATEST, null, 2)

            assertEquals(2, result.size)
        }
    }

    @Nested
    inner class 글_단위_삭제 {

        @Test
        fun `해당 글의 댓글만 전부 삭제하고 다른 글 댓글은 보존한다`() {
            saveComment(postId = 100L)
            saveComment(postId = 100L)
            val other = saveComment(postId = 200L)

            commentRepository.deleteByPostId(100L)

            assertEquals(0, commentRepository.findByPost(100L, CommentSort.LATEST, null, 10).size)
            assertEquals(listOf(other.id), commentRepository.findByPost(200L, CommentSort.LATEST, null, 10).map { it.id })
        }
    }
}
