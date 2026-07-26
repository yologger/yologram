package link.yologram.api.v1.domain.ums.exception

open class UmsException(override val message: String, val errorCode: String) : RuntimeException(message)

class UserDuplicateException : UmsException("이미 등록된 이메일입니다.", "USER_DUPLICATE")
class UserNotFoundException : UmsException("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND")
class AdminUserDuplicateException : UmsException("이미 등록된 어드민 이메일입니다.", "ADMIN_USER_DUPLICATE")
class AdminUserNotFoundException : UmsException("어드민 사용자를 찾을 수 없습니다.", "ADMIN_USER_NOT_FOUND")
class AuthWrongPasswordException : UmsException("비밀번호가 올바르지 않습니다.", "AUTH_WRONG_PASSWORD")
class AuthTokenExpiredException : UmsException("토큰이 만료되었습니다.", "AUTH_EXPIRED_TOKEN")
class AuthTokenInvalidException : UmsException("유효하지 않은 토큰입니다.", "AUTH_INVALID_TOKEN")
class UserEmailVerificationExpiredException : UmsException("인증 코드가 만료되었습니다.", "USER_EMAIL_VERIFICATION_EXPIRED")
class UserEmailVerificationInvalidException : UmsException("인증 코드가 일치하지 않습니다.", "USER_EMAIL_VERIFICATION_INVALID")
class UserEmailNotVerifiedException : UmsException("이메일 인증이 완료되지 않았습니다.", "USER_EMAIL_NOT_VERIFIED")
class UserPasswordResetExpiredException : UmsException("인증 코드가 만료되었습니다.", "USER_PASSWORD_RESET_EXPIRED")
class UserPasswordResetInvalidException : UmsException("인증 코드가 일치하지 않습니다.", "USER_PASSWORD_RESET_INVALID")
