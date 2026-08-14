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


class AdminUserSelfDeleteException(AppException):
    def __init__(self):
        super().__init__(400, "자기 자신은 삭제할 수 없습니다.", "ADMIN_USER_SELF_DELETE")


class AdminUserOwnerUndeletableException(AppException):
    def __init__(self):
        super().__init__(400, "OWNER 계정은 삭제할 수 없습니다.", "ADMIN_USER_OWNER_UNDELETABLE")


class AdminRoleForbiddenException(AppException):
    def __init__(self):
        super().__init__(403, "OWNER만 가능한 작업입니다.", "ADMIN_ROLE_FORBIDDEN")


class AdminUserOwnerImmutableException(AppException):
    def __init__(self):
        super().__init__(400, "OWNER 계정은 변경할 수 없습니다.", "ADMIN_USER_OWNER_IMMUTABLE")


class AdminUserInactiveException(AppException):
    def __init__(self):
        super().__init__(403, "비활성화된 계정입니다.", "ADMIN_USER_INACTIVE")


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


class InvalidIndexRangeException(AppException):
    def __init__(self):
        super().__init__(400, "인덱싱 범위가 유효하지 않습니다.", "INVALID_INDEX_RANGE")


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


class BlankSearchKeywordException(AppException):
    def __init__(self):
        super().__init__(400, "검색어를 입력해주세요.", "BLANK_SEARCH_KEYWORD")


class SearchPageTooDeepException(AppException):
    """
    OpenSearch의 max_result_window(기본 10000) 초과 — from + size가 그 값을 넘으면 엔진이 예외를 낸다.
    막지 않으면 500이 되므로 400으로 돌려준다(요청이 잘못된 것이지 서버 오류가 아니다).
    """

    def __init__(self):
        super().__init__(400, "더 이상 조회할 수 없는 페이지입니다.", "SEARCH_PAGE_TOO_DEEP")


class SearchUnavailableException(AppException):
    """
    검색 설정(opensearch.main.*)이 없는 환경에서 검색을 호출한 경우.
    라우터는 항상 등록한다(클라이언트가 lazy라 설정 없이도 부팅된다) — 대신 호출 시점에 막고
    "설정이 없다"는 것을 503으로 알린다(엔진 접속 실패로 500이 나는 것보다 원인이 분명하다).
    """

    def __init__(self):
        super().__init__(503, "검색 기능을 사용할 수 없습니다.", "SEARCH_UNAVAILABLE")
