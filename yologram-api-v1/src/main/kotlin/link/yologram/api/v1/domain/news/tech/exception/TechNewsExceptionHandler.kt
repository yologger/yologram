package link.yologram.api.v1.domain.news.tech.exception

import link.yologram.api.v1.global.exception.ErrorResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["link.yologram.api.v1.domain.news.tech"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class TechNewsExceptionHandler {

    @ExceptionHandler(InvalidTechNewsCursorException::class)
    fun handleInvalidCursor(e: InvalidTechNewsCursorException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }
}
