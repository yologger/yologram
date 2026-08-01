package link.yologram.api.v1.domain.comment.tech.exception

import link.yologram.api.v1.global.exception.ErrorResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["link.yologram.api.v1.domain.comment.tech"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class TechPostCommentExceptionHandler {

    @ExceptionHandler(TargetTechPostNotFoundException::class)
    fun handleTargetPostNotFound(e: TargetTechPostNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(InvalidTechPostCommentCursorException::class)
    fun handleInvalidCursor(e: InvalidTechPostCommentCursorException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(TechPostCommentNotFoundException::class)
    fun handleCommentNotFound(e: TechPostCommentNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(TechPostCommentForbiddenException::class)
    fun handleCommentForbidden(e: TechPostCommentForbiddenException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse(e.message, e.errorCode))
    }
}
