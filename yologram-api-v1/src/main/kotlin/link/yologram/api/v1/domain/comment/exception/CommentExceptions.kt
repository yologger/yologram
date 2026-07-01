package link.yologram.api.v1.domain.comment.exception

open class CommentException(override val message: String, val errorCode: String) : RuntimeException(message)

class TargetPostNotFoundException : CommentException("대상 게시글을 찾을 수 없습니다.", "POST_NOT_FOUND")

class InvalidCommentCursorException : CommentException("유효하지 않은 커서입니다.", "INVALID_CURSOR")
