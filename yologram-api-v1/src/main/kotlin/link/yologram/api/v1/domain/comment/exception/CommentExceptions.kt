package link.yologram.api.v1.domain.comment.exception

open class CommentException(override val message: String, val errorCode: String) : RuntimeException(message)

class TargetPostNotFoundException : CommentException("대상 게시글을 찾을 수 없습니다.", "POST_NOT_FOUND")

class InvalidCommentCursorException : CommentException("유효하지 않은 커서입니다.", "INVALID_CURSOR")

class CommentNotFoundException : CommentException("댓글을 찾을 수 없습니다.", "COMMENT_NOT_FOUND")

class CommentForbiddenException : CommentException("본인 댓글만 수정·삭제할 수 있습니다.", "COMMENT_FORBIDDEN")
