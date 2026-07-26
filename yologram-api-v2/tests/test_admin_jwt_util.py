import os
import time

import pytest

os.environ["JWT_SECRET"] = "test-jwt-secret-key-for-testing"
os.environ["ADMIN_JWT_SECRET"] = "test-admin-jwt-secret-key-for-testing"

from app.config.settings import get_settings
from app.core.exception import AuthTokenExpiredException, AuthTokenInvalidException
from app.domain.ums.admin_jwt_util import create_admin_token, validate_and_get_admin_uid
from app.domain.ums.jwt_util import create_token, validate_and_get_uid


# lru_cache 초기화
get_settings.cache_clear()


class TestAdminJwtUtil:

    def test_토큰_생성_후_uid_추출(self):
        token = create_admin_token(42)
        uid = validate_and_get_admin_uid(token)
        assert uid == 42

    def test_유효하지_않은_토큰(self):
        with pytest.raises(AuthTokenInvalidException):
            validate_and_get_admin_uid("invalid-token")

    def test_다른_secret으로_서명된_토큰(self):
        import jwt as pyjwt

        token = pyjwt.encode({"uid": 1}, "wrong-secret", algorithm="HS256")
        with pytest.raises(AuthTokenInvalidException):
            validate_and_get_admin_uid(token)

    def test_만료된_토큰(self):
        import jwt as pyjwt

        settings = get_settings()
        payload = {
            "uid": 1,
            "iss": settings.admin_jwt_issuer,
            "aud": settings.admin_jwt_audience,
            "iat": time.time() - 100,
            "exp": time.time() - 10,
        }
        token = pyjwt.encode(payload, settings.admin_jwt_secret, algorithm="HS256")
        with pytest.raises(AuthTokenExpiredException):
            validate_and_get_admin_uid(token)

    def test_유저_토큰은_어드민_검증에서_거부(self):
        # 유저 JWT는 secret·audience가 달라 어드민 검증을 통과할 수 없음
        user_token = create_token(1)
        with pytest.raises(AuthTokenInvalidException):
            validate_and_get_admin_uid(user_token)

    def test_어드민_토큰은_유저_검증에서_거부(self):
        admin_token = create_admin_token(1)
        with pytest.raises(AuthTokenInvalidException):
            validate_and_get_uid(admin_token)
