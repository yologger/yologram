from datetime import datetime, timedelta, timezone

import jwt

from app.config.settings import get_settings


def create_admin_token(uid: int) -> str:
    settings = get_settings()
    now = datetime.now(timezone.utc)
    payload = {
        "uid": uid,
        "iss": settings.admin_jwt_issuer,
        "aud": settings.admin_jwt_audience,
        "iat": now,
        "exp": now + timedelta(seconds=settings.admin_jwt_expire),
    }
    return jwt.encode(payload, settings.admin_jwt_secret, algorithm="HS256")


def validate_and_get_admin_uid(token: str) -> int:
    from app.core.exception import AuthTokenExpiredException, AuthTokenInvalidException

    settings = get_settings()
    try:
        payload = jwt.decode(
            token,
            settings.admin_jwt_secret,
            algorithms=["HS256"],
            issuer=settings.admin_jwt_issuer,
            audience=settings.admin_jwt_audience,
        )
        return payload["uid"]
    except jwt.ExpiredSignatureError:
        raise AuthTokenExpiredException()
    except jwt.PyJWTError:
        raise AuthTokenInvalidException()
