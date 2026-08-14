package link.yologram.api.v1.domain.search.exception

open class SearchException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidIndexRangeException : SearchException("인덱싱 범위가 유효하지 않습니다.", "INVALID_INDEX_RANGE")
