from fastapi import Depends, Request
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.exception import AuthTokenInvalidException
from app.domain.ums.auth_schema import AuthData
from app.domain.ums.jwt_util import validate_and_get_uid


def get_authenticated_user(request: Request) -> AuthData:
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer ") or len(auth_header) <= 7:
        raise AuthTokenInvalidException()

    token = auth_header[7:].strip()
    if not token:
        raise AuthTokenInvalidException()

    uid = validate_and_get_uid(token)
    return AuthData(uid=uid, access_token=token)


def get_optional_authenticated_user(request: Request) -> AuthData | None:
    """선택 인증 — 공개 API지만 로그인 시 개인화 값(likedByMe 등)을 채우는 곳에서 사용
    (api-v1 OptionalAuthenticatedUserResolver 미러).
    Authorization 헤더가 없으면 None(비로그인 취급), 있으면 필수 인증과 동일하게 검증(무효 토큰 401) —
    "틀린 토큰인데 비로그인으로 조용히 처리"는 클라이언트 버그(만료 토큰 방치)를 숨기므로 하지 않는다."""
    if request.headers.get("Authorization") is None:
        return None
    return get_authenticated_user(request)
