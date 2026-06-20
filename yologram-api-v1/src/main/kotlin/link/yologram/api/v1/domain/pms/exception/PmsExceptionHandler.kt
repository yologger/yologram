package link.yologram.api.v1.domain.pms.exception

import link.yologram.api.v1.domain.cms.exception.InvalidSectionException
import link.yologram.api.v1.global.exception.ErrorResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["link.yologram.api.v1.domain.pms"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class PmsExceptionHandler {

    @ExceptionHandler(InvalidCategoryException::class)
    fun handleInvalidCategory(e: InvalidCategoryException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(PostNotFoundException::class)
    fun handlePostNotFound(e: PostNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(InvalidCursorException::class)
    fun handleInvalidCursor(e: InvalidCursorException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    // 게시글 작성 경로(/pms/{section})에서 잘못된 section path 처리 (Section.fromPath)
    @ExceptionHandler(InvalidSectionException::class)
    fun handleInvalidSection(e: InvalidSectionException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }
}
