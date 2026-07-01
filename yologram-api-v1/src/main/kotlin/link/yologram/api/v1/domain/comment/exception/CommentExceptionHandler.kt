package link.yologram.api.v1.domain.comment.exception

import link.yologram.api.v1.global.exception.ErrorResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["link.yologram.api.v1.domain.comment"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class CommentExceptionHandler {

    @ExceptionHandler(TargetPostNotFoundException::class)
    fun handleTargetPostNotFound(e: TargetPostNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(InvalidCommentCursorException::class)
    fun handleInvalidCursor(e: InvalidCommentCursorException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }
}
