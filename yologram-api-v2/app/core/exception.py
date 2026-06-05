import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

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


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppException)
    async def app_exception_handler(_request: Request, exc: AppException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={"errorMessage": exc.error_message, "errorCode": exc.error_code},
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
