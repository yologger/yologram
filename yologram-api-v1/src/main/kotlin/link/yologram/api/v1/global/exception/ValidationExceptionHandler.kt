package link.yologram.api.v1.global.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.error { e.message }
        val fieldErrors = e.bindingResult.fieldErrors
        val fieldError = fieldErrors[fieldErrors.size - 1]
        val errorMessage = fieldError.defaultMessage ?: "${fieldError.field} field has invalid value"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(errorMessage = errorMessage, errorCode = "VALIDATION_ERROR"))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handle(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        logger.error { e.message }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(errorMessage = e.message, errorCode = "VALIDATION_ERROR"))
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handle(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> {
        logger.error { e.message }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(errorMessage = e.message, errorCode = "VALIDATION_ERROR"))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(e: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        logger.error { e.message }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(errorMessage = e.message, errorCode = "VALIDATION_ERROR"))
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handle(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        logger.error { e.message }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(errorMessage = e.message, errorCode = "VALIDATION_ERROR"))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.error { e.message }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(errorMessage = "Json parse error", errorCode = "VALIDATION_ERROR"))
    }
}
