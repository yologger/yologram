from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse


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
