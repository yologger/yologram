package link.yologram.api.v1.domain.cms.exception

open class CmsException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidSectionException : CmsException("유효하지 않은 섹션입니다.", "INVALID_SECTION")
