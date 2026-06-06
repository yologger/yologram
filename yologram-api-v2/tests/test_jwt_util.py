import os
import time

import pytest

os.environ["JWT_SECRET"] = "test-jwt-secret-key-for-testing"

from app.config.settings import get_settings
from app.core.exception import AuthTokenExpiredException, AuthTokenInvalidException
from app.domain.ums.jwt_util import create_token, validate_and_get_uid


# lru_cache 초기화
get_settings.cache_clear()


class TestJwtUtil:

    def test_토큰_생성_후_uid_추출(self):
        token = create_token(42)
        uid = validate_and_get_uid(token)
        assert uid == 42

    def test_유효하지_않은_토큰(self):
        with pytest.raises(AuthTokenInvalidException):
            validate_and_get_uid("invalid-token")

    def test_다른_secret으로_서명된_토큰(self):
        import jwt as pyjwt

        token = pyjwt.encode({"uid": 1}, "wrong-secret", algorithm="HS256")
        with pytest.raises(AuthTokenInvalidException):
            validate_and_get_uid(token)

    def test_만료된_토큰(self):
        import jwt as pyjwt

        settings = get_settings()
        payload = {
            "uid": 1,
            "iss": settings.jwt_issuer,
            "aud": settings.jwt_audience,
            "iat": time.time() - 100,
            "exp": time.time() - 10,
        }
        token = pyjwt.encode(payload, settings.jwt_secret, algorithm="HS256")
        with pytest.raises(AuthTokenExpiredException):
            validate_and_get_uid(token)
