package link.yologram.api.v1.domain.tech.news.exception

open class TechNewsException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidTechNewsCursorException : TechNewsException("유효하지 않은 커서입니다.", "INVALID_CURSOR")
