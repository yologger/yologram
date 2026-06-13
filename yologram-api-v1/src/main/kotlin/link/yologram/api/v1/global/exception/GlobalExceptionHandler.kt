package link.yologram.api.v1.global.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class GlobalExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handle(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
        logger.error { e.message }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ErrorResponse(errorMessage = "Method Not Allowed", errorCode = "METHOD_NOT_ALLOWED"))
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handle(e: NoHandlerFoundException): ResponseEntity<ErrorResponse> {
        logger.warn { e.message }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(errorMessage = "Not Found", errorCode = "NOT_FOUND"))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handle(e: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        logger.warn { e.message }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(errorMessage = "Not Found", errorCode = "NOT_FOUND"))
    }

    @ExceptionHandler(Exception::class)
    fun handle(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error { "${e::class.simpleName}: ${e.message}" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(errorMessage = "Internal Server Error", errorCode = "INTERNAL_SERVER_ERROR"))
    }
}
