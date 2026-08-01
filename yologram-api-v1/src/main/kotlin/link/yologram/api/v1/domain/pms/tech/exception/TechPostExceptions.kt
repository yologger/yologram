package link.yologram.api.v1.domain.pms.tech.exception

open class TechPostException(override val message: String, val errorCode: String) : RuntimeException(message)

class InvalidTechCategoryException : TechPostException("해당 게시판의 카테고리가 아닙니다.", "INVALID_POST_CATEGORY")

class TechPostNotFoundException : TechPostException("게시글을 찾을 수 없습니다.", "POST_NOT_FOUND")

class InvalidTechPostCursorException : TechPostException("유효하지 않은 커서입니다.", "INVALID_CURSOR")

class TechPostForbiddenException : TechPostException("본인 게시글만 수정·삭제할 수 있습니다.", "POST_FORBIDDEN")

/** 내 글 목록의 section 쿼리 파라미터 검증용 — tech 외 값이면 400 (구 Section enum 검증과 동일 응답) */
class InvalidTechSectionException : TechPostException("유효하지 않은 섹션입니다.", "INVALID_SECTION")
