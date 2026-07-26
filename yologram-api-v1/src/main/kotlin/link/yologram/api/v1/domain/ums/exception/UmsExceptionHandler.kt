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

    @ExceptionHandler(AdminUserDuplicateException::class)
    fun handleAdminUserDuplicate(e: AdminUserDuplicateException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(AdminUserNotFoundException::class)
    fun handleAdminUserNotFound(e: AdminUserNotFoundException): ResponseEntity<ErrorResponse> {
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

    @ExceptionHandler(UserEmailVerificationExpiredException::class)
    fun handleEmailVerificationExpired(e: UserEmailVerificationExpiredException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(UserEmailVerificationInvalidException::class)
    fun handleEmailVerificationInvalid(e: UserEmailVerificationInvalidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(UserEmailNotVerifiedException::class)
    fun handleEmailNotVerified(e: UserEmailNotVerifiedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(UserPasswordResetExpiredException::class)
    fun handleUserPasswordResetExpired(e: UserPasswordResetExpiredException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }

    @ExceptionHandler(UserPasswordResetInvalidException::class)
    fun handleUserPasswordResetInvalid(e: UserPasswordResetInvalidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message, e.errorCode))
    }
}
