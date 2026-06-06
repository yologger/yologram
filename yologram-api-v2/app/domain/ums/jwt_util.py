from datetime import datetime, timedelta, timezone

import jwt

from app.config.settings import get_settings


def create_token(uid: int) -> str:
    settings = get_settings()
    now = datetime.now(timezone.utc)
    payload = {
        "uid": uid,
        "iss": settings.jwt_issuer,
        "aud": settings.jwt_audience,
        "iat": now,
        "exp": now + timedelta(seconds=settings.jwt_expire),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm="HS256")


def validate_and_get_uid(token: str) -> int:
    from app.core.exception import AuthTokenExpiredException, AuthTokenInvalidException

    settings = get_settings()
    try:
        payload = jwt.decode(
            token,
            settings.jwt_secret,
            algorithms=["HS256"],
            issuer=settings.jwt_issuer,
            audience=settings.jwt_audience,
        )
        return payload["uid"]
    except jwt.ExpiredSignatureError:
        raise AuthTokenExpiredException()
    except jwt.PyJWTError:
        raise AuthTokenInvalidException()
