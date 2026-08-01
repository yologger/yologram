package link.yologram.api.v1.domain.pms.tech.exception

import link.yologram.api.v1.global.exception.ErrorResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["link.yologram.api.v1.domain.pms.tech"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class TechPostExceptionHandler {

    @ExceptionHandler(InvalidTechCategoryException::class)
    fun handleInvalidCategory(e: InvalidTechCategoryException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(TechPostNotFoundException::class)
    fun handlePostNotFound(e: TechPostNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(InvalidTechPostCursorException::class)
    fun handleInvalidCursor(e: InvalidTechPostCursorException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(TechPostForbiddenException::class)
    fun handlePostForbidden(e: TechPostForbiddenException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse(e.message, e.errorCode))
    }

    // 내 글 목록(/pms/posts/me)의 section 쿼리 파라미터가 tech 외 값일 때 400
    @ExceptionHandler(InvalidTechSectionException::class)
    fun handleInvalidSection(e: InvalidTechSectionException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }
}
