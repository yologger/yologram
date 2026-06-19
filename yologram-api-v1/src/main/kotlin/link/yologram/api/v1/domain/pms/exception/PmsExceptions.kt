package link.yologram.api.v1.domain.pms.exception

open class PmsException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidCategoryException : PmsException("해당 게시판의 카테고리가 아닙니다.", "INVALID_CATEGORY")

class PostNotFoundException : PmsException("게시글을 찾을 수 없습니다.", "POST_NOT_FOUND")
