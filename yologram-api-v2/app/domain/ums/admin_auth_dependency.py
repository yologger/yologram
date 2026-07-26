from fastapi import Request

from app.core.exception import AuthTokenInvalidException
from app.domain.ums.admin_jwt_util import validate_and_get_admin_uid
from app.domain.ums.admin_schema import AdminAuthData


def get_authenticated_admin(request: Request) -> AdminAuthData:
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer ") or len(auth_header) <= 7:
        raise AuthTokenInvalidException()

    token = auth_header[7:].strip()
    if not token:
        raise AuthTokenInvalidException()

    uid = validate_and_get_admin_uid(token)
    return AdminAuthData(uid=uid, access_token=token)
