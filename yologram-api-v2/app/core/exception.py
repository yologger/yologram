import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

logger = logging.getLogger(__name__)


class AppException(Exception):
    def __init__(self, status_code: int, error_message: str, error_code: str):
        self.status_code = status_code
        self.error_message = error_message
        self.error_code = error_code


class UserDuplicateException(AppException):
    def __init__(self):
        super().__init__(409, "이미 가입된 이메일입니다.", "USER_DUPLICATE")


class UserNotFoundException(AppException):
    def __init__(self):
        super().__init__(404, "존재하지 않는 사용자입니다.", "USER_NOT_FOUND")


class AdminUserDuplicateException(AppException):
    def __init__(self):
        super().__init__(409, "이미 등록된 어드민 이메일입니다.", "ADMIN_USER_DUPLICATE")


class AdminUserNotFoundException(AppException):
    def __init__(self):
        super().__init__(404, "어드민 사용자를 찾을 수 없습니다.", "ADMIN_USER_NOT_FOUND")


class AuthWrongPasswordException(AppException):
    def __init__(self):
        super().__init__(401, "비밀번호가 일치하지 않습니다.", "AUTH_WRONG_PASSWORD")


class AuthTokenExpiredException(AppException):
    def __init__(self):
        super().__init__(401, "토큰이 만료되었습니다.", "AUTH_EXPIRED_TOKEN")


class AuthTokenInvalidException(AppException):
    def __init__(self):
        super().__init__(401, "유효하지 않은 토큰입니다.", "AUTH_INVALID_TOKEN")


class UserEmailVerificationExpiredException(AppException):
    def __init__(self):
        super().__init__(400, "인증 코드가 만료되었습니다.", "USER_EMAIL_VERIFICATION_EXPIRED")


class UserEmailVerificationInvalidException(AppException):
    def __init__(self):
        super().__init__(400, "인증 코드가 일치하지 않습니다.", "USER_EMAIL_VERIFICATION_INVALID")


class UserEmailNotVerifiedException(AppException):
    def __init__(self):
        super().__init__(400, "이메일 인증이 완료되지 않았습니다.", "USER_EMAIL_NOT_VERIFIED")


class UserPasswordResetExpiredException(AppException):
    def __init__(self):
        super().__init__(400, "인증 코드가 만료되었습니다.", "USER_PASSWORD_RESET_EXPIRED")


class UserPasswordResetInvalidException(AppException):
    def __init__(self):
        super().__init__(400, "인증 코드가 일치하지 않습니다.", "USER_PASSWORD_RESET_INVALID")


class InvalidSectionException(AppException):
    def __init__(self):
        super().__init__(400, "유효하지 않은 섹션입니다.", "INVALID_SECTION")


class InvalidPostCategoryException(AppException):
    def __init__(self):
        super().__init__(400, "해당 게시판의 카테고리가 아닙니다.", "INVALID_POST_CATEGORY")


class PostNotFoundException(AppException):
    def __init__(self):
        super().__init__(404, "게시글을 찾을 수 없습니다.", "POST_NOT_FOUND")


class InvalidCursorException(AppException):
    def __init__(self):
        super().__init__(400, "유효하지 않은 커서입니다.", "INVALID_CURSOR")


class PostForbiddenException(AppException):
    def __init__(self):
        super().__init__(403, "본인 게시글만 수정·삭제할 수 있습니다.", "POST_FORBIDDEN")


class TargetPostNotFoundException(AppException):
    def __init__(self):
        super().__init__(404, "대상 게시글을 찾을 수 없습니다.", "POST_NOT_FOUND")


class CommentNotFoundException(AppException):
    def __init__(self):
        super().__init__(404, "댓글을 찾을 수 없습니다.", "COMMENT_NOT_FOUND")


class CommentForbiddenException(AppException):
    def __init__(self):
        super().__init__(403, "본인 댓글만 수정·삭제할 수 있습니다.", "COMMENT_FORBIDDEN")


class TechNewsSourceNotFoundException(AppException):
    def __init__(self):
        super().__init__(404, "뉴스 소스를 찾을 수 없습니다.", "NEWS_SOURCE_NOT_FOUND")


class TechNewsSourceDuplicateException(AppException):
    def __init__(self):
        super().__init__(409, "이미 등록된 뉴스 소스 URL입니다.", "NEWS_SOURCE_DUPLICATE")


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppException)
    async def app_exception_handler(_request: Request, exc: AppException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={"errorMessage": exc.error_message, "errorCode": exc.error_code},
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(_request: Request, exc: StarletteHTTPException) -> JSONResponse:
        code_map = {404: "NOT_FOUND", 405: "METHOD_NOT_ALLOWED"}
        error_code = code_map.get(exc.status_code, "HTTP_ERROR")
        return JSONResponse(
            status_code=exc.status_code,
            content={"errorMessage": exc.detail, "errorCode": error_code},
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(_request: Request, exc: RequestValidationError) -> JSONResponse:
        errors = exc.errors()
        message = errors[0].get("msg") if errors else "잘못된 요청입니다."
        # Pydantic field_validator의 ValueError는 "Value error, " 접두가 붙음 → 제거
        prefix = "Value error, "
        if isinstance(message, str) and message.startswith(prefix):
            message = message[len(prefix):]
        return JSONResponse(
            status_code=400,
            content={"errorMessage": message, "errorCode": "VALIDATION_ERROR"},
        )

    @app.exception_handler(Exception)
    async def global_exception_handler(_request: Request, exc: Exception) -> JSONResponse:
        logger.error(f"Unhandled exception: {exc}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"errorMessage": "서버 내부 오류가 발생했습니다.", "errorCode": "INTERNAL_SERVER_ERROR"},
        )
