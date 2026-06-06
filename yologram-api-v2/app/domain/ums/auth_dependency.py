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
