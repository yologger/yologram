package link.yologram.api.v1.domain.comment.exception

open class CommentException(override val message: String, val errorCode: String) : RuntimeException(message)

class TargetPostNotFoundException : CommentException("대상 게시글을 찾을 수 없습니다.", "POST_NOT_FOUND")
