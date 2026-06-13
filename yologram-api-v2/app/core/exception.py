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


class AuthWrongPasswordException(AppException):
    def __init__(self):
        super().__init__(401, "비밀번호가 일치하지 않습니다.", "AUTH_WRONG_PASSWORD")


class AuthTokenExpiredException(AppException):
    def __init__(self):
        super().__init__(401, "토큰이 만료되었습니다.", "AUTH_TOKEN_EXPIRED")


class AuthTokenInvalidException(AppException):
    def __init__(self):
        super().__init__(401, "유효하지 않은 토큰입니다.", "AUTH_TOKEN_INVALID")


class EmailVerificationExpiredException(AppException):
    def __init__(self):
        super().__init__(400, "인증 코드가 만료되었습니다.", "EMAIL_VERIFICATION_EXPIRED")


class EmailVerificationInvalidException(AppException):
    def __init__(self):
        super().__init__(400, "인증 코드가 일치하지 않습니다.", "EMAIL_VERIFICATION_INVALID")


class EmailNotVerifiedException(AppException):
    def __init__(self):
        super().__init__(400, "이메일 인증이 완료되지 않았습니다.", "EMAIL_NOT_VERIFIED")


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
        return JSONResponse(
            status_code=422,
            content={"errorMessage": str(exc.errors()), "errorCode": "VALIDATION_ERROR"},
        )

    @app.exception_handler(Exception)
    async def global_exception_handler(_request: Request, exc: Exception) -> JSONResponse:
        logger.error(f"Unhandled exception: {exc}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"errorMessage": "서버 내부 오류가 발생했습니다.", "errorCode": "INTERNAL_SERVER_ERROR"},
        )
