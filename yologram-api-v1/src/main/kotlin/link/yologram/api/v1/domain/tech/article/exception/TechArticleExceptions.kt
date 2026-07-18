package link.yologram.api.v1.domain.tech.article.exception

open class TechArticleException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidTechArticleCursorException : TechArticleException("유효하지 않은 커서입니다.", "INVALID_CURSOR")
