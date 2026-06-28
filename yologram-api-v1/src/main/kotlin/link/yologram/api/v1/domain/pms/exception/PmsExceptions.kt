package link.yologram.api.v1.domain.pms.exception

open class PmsException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidPostCategoryException : PmsException("해당 게시판의 카테고리가 아닙니다.", "INVALID_POST_CATEGORY")

class PostNotFoundException : PmsException("게시글을 찾을 수 없습니다.", "POST_NOT_FOUND")

class InvalidCursorException : PmsException("유효하지 않은 커서입니다.", "INVALID_CURSOR")

class PostForbiddenException : PmsException("본인 게시글만 수정·삭제할 수 있습니다.", "POST_FORBIDDEN")
