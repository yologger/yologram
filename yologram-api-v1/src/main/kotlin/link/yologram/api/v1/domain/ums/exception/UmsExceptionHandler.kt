package link.yologram.api.v1.domain.ums.exception

import link.yologram.api.v1.global.exception.ErrorResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["link.yologram.api.v1.domain.ums"])
@Order(Ordered.HIGHEST_PRECEDENCE)
class UmsExceptionHandler {

    @ExceptionHandler(UserDuplicateException::class)
    fun handleUserDuplicate(e: UserDuplicateException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(AuthWrongPasswordException::class)
    fun handleWrongPassword(e: AuthWrongPasswordException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(AuthTokenExpiredException::class)
    fun handleTokenExpired(e: AuthTokenExpiredException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(AuthTokenInvalidException::class)
    fun handleTokenInvalid(e: AuthTokenInvalidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(EmailVerificationExpiredException::class)
    fun handleEmailVerificationExpired(e: EmailVerificationExpiredException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(EmailVerificationInvalidException::class)
    fun handleEmailVerificationInvalid(e: EmailVerificationInvalidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(EmailNotVerifiedException::class)
    fun handleEmailNotVerified(e: EmailNotVerifiedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(PasswordResetExpiredException::class)
    fun handlePasswordResetExpired(e: PasswordResetExpiredException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(PasswordResetInvalidException::class)
    fun handlePasswordResetInvalid(e: PasswordResetInvalidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }
}
