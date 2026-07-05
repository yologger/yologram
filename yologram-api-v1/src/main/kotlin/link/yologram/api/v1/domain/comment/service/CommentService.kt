package link.yologram.api.v1.domain.comment.service

import link.yologram.api.v1.domain.comment.entity.Comment
import link.yologram.api.v1.domain.comment.exception.CommentForbiddenException
import link.yologram.api.v1.domain.comment.exception.CommentNotFoundException
import link.yologram.api.v1.domain.comment.exception.TargetPostNotFoundException
import link.yologram.api.v1.domain.comment.model.CommentCursor
import link.yologram.api.v1.domain.comment.model.CommentResponse
import link.yologram.api.v1.domain.comment.model.CommentSort
import link.yologram.api.v1.domain.comment.model.CreateCommentRequest
import link.yologram.api.v1.domain.comment.model.CreateCommentResponse
import link.yologram.api.v1.domain.comment.model.UpdateCommentRequest
import link.yologram.api.v1.domain.comment.repository.CommentRepository
import link.yologram.api.v1.global.model.ApiEnvelopCursorPage
import link.yologram.api.v1.global.model.ApiEnvelopPage
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postQueryClient: PostQueryClient,
    private val userQueryClient: UserQueryClient,
) {

    companion object {
        private const val MAX_PAGE_SIZE = 50
    }

    // 댓글 작성
    @Transactional
    fun create(postId: Long, userId: Long, request: CreateCommentRequest): CreateCommentResponse {
        // 대상 글이 없으면 404 (고아 댓글 방지)
        if (!postQueryClient.exists(postId)) throw TargetPostNotFoundException()

        val comment = commentRepository.save(
            Comment(
                postId = postId,
                userId = userId,
                content = request.content!!,
            )
        )
        return CreateCommentResponse(id = comment.id)
    }

    // 댓글 수정 (본인 댓글)
    @Transactional
    fun update(commentId: Long, userId: Long, request: UpdateCommentRequest) {
        val comment = commentRepository.findByIdOrNull(commentId) ?: throw CommentNotFoundException()
        // 작성자 본인만 수정 가능 (아니면 403)
        if (comment.userId != userId) throw CommentForbiddenException()

        // 내용 갱신 (JPA 더티체킹 → flush 시 update, modifiedDate 자동 갱신)
        comment.update(request.content!!)
    }

    /**
     * 특정 글의 댓글 목록 조회 (cursor 페이지네이션, 최신순/오래된순).
     * 없는 postId면 빈 목록을 반환한다(존재 검증은 작성 시에만).
     */
    @Transactional(readOnly = true)
    fun getCommentsByCursor(postId: Long, sortParam: String?, cursor: String?, size: Int): ApiEnvelopCursorPage<CommentResponse> {
        val sort = CommentSort.fromParam(sortParam)
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val cursorId = cursor?.let { CommentCursor.decode(it) }

        val comments = commentRepository.findByPost(postId, sort, cursorId, pageSize)

        // 작성자 닉네임 배치 조회 (N+1 회피)
        val nicknames = userQueryClient.findNicknames(comments.map { it.userId })

        val data = comments.map { comment ->
            CommentResponse(
                id = comment.id,
                postId = comment.postId,
                author = CommentResponse.Author(uid = comment.userId, nickname = nicknames[comment.userId]),
                content = comment.content,
                createdAt = comment.createdAt,
            )
        }

        val nextCursor = comments.lastOrNull()?.let { CommentCursor.encode(it.id) }
        return ApiEnvelopCursorPage(data = data, nextCursor = nextCursor)
    }

    /**
     * 특정 글의 댓글 목록 조회 (offset 페이지네이션) — 학습용.
     * cursor 방식(getComments)과 대비되는 offset + 전체 count 예시. 엔드포인트는 비활성(Resource 주석).
     */
    @Transactional(readOnly = true)
    fun getCommentsByOffset(postId: Long, sortParam: String?, page: Int, size: Int): ApiEnvelopPage<CommentResponse> {
        val sort = CommentSort.fromParam(sortParam)
        val pageNumber = page.coerceAtLeast(0)
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val offset = pageNumber.toLong() * pageSize

        val totalCount = commentRepository.countByPost(postId)
        val comments = commentRepository.findByPost(postId, sort, offset, pageSize)

        val nicknames = userQueryClient.findNicknames(comments.map { it.userId })

        val data = comments.map { comment ->
            CommentResponse(
                id = comment.id,
                postId = comment.postId,
                author = CommentResponse.Author(uid = comment.userId, nickname = nicknames[comment.userId]),
                content = comment.content,
                createdAt = comment.createdAt,
            )
        }

        val totalPages = if (totalCount == 0L) 0L else (totalCount + pageSize - 1) / pageSize
        return ApiEnvelopPage(
            data = data,
            page = pageNumber.toLong(),
            size = pageSize.toLong(),
            totalPages = totalPages,
            totalCount = totalCount,
            first = pageNumber == 0,
            last = totalPages == 0L || pageNumber.toLong() >= totalPages - 1,
        )
    }
}
