package link.yologram.api.v1.domain.news.tech.exception

open class TechNewsException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidTechNewsCursorException : TechNewsException("유효하지 않은 커서입니다.", "INVALID_CURSOR")

class TechNewsSourceNotFoundException : TechNewsException("뉴스 소스를 찾을 수 없습니다.", "NEWS_SOURCE_NOT_FOUND")

class TechNewsSourceDuplicateException : TechNewsException("이미 등록된 뉴스 소스 URL입니다.", "NEWS_SOURCE_DUPLICATE")
