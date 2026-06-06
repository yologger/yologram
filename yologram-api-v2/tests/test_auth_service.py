import os
from unittest.mock import MagicMock, patch

import bcrypt
import pytest

os.environ["JWT_SECRET"] = "test-jwt-secret-key-for-testing"

from app.core.exception import AuthTokenInvalidException, AuthWrongPasswordException, UserNotFoundException
from app.domain.ums.auth_schema import AuthData, LoginRequest
from app.domain.ums.auth_service import AuthService
from app.domain.ums.model import User


class TestAuthService:

    class TestLogin:

        def setup_method(self):
            self.db = MagicMock()
            self.service = AuthService(self.db)
            self.hashed_pw = bcrypt.hashpw("password123!".encode("utf-8"), bcrypt.gensalt()).decode("utf-8")
            self.user = User(email="test@yologram.link", name="테스터", nickname="tester", password=self.hashed_pw)
            self.user.id = 1

        def test_로그인_성공(self):
            self.service.repository.find_by_email = MagicMock(return_value=self.user)
            request = LoginRequest(email="test@yologram.link", password="password123!")

            result = self.service.login(request)

            assert result.uid == 1
            assert result.email == "test@yologram.link"
            assert result.name == "테스터"
            assert result.nickname == "tester"
            assert result.access_token is not None

        def test_존재하지_않는_사용자(self):
            self.service.repository.find_by_email = MagicMock(return_value=None)
            request = LoginRequest(email="notfound@yologram.link", password="password123!")

            with pytest.raises(UserNotFoundException):
                self.service.login(request)

        def test_비밀번호_불일치(self):
            self.service.repository.find_by_email = MagicMock(return_value=self.user)
            request = LoginRequest(email="test@yologram.link", password="wrongpassword")

            with pytest.raises(AuthWrongPasswordException):
                self.service.login(request)

    class TestValidateToken:

        def setup_method(self):
            self.db = MagicMock()
            self.service = AuthService(self.db)
            self.user = User(email="test@yologram.link", name="테스터", nickname="tester", password="hashed")
            self.user.id = 1
            self.user.access_token = "valid-token"

        def test_토큰_검증_성공(self):
            self.service.repository.find_by_id = MagicMock(return_value=self.user)
            auth_data = AuthData(uid=1, access_token="valid-token")

            result = self.service.validate_token(auth_data)

            assert result.uid == 1
            assert result.email == "test@yologram.link"

        def test_DB_토큰_불일치(self):
            self.service.repository.find_by_id = MagicMock(return_value=self.user)
            auth_data = AuthData(uid=1, access_token="different-token")

            with pytest.raises(AuthTokenInvalidException):
                self.service.validate_token(auth_data)

        def test_사용자_없음(self):
            self.service.repository.find_by_id = MagicMock(return_value=None)
            auth_data = AuthData(uid=999, access_token="any-token")

            with pytest.raises(UserNotFoundException):
                self.service.validate_token(auth_data)

    class TestLogout:

        def setup_method(self):
            self.db = MagicMock()
            self.service = AuthService(self.db)
            self.user = User(email="test@yologram.link", name="테스터", nickname="tester", password="hashed")
            self.user.id = 1
            self.user.access_token = "valid-token"

        def test_로그아웃_성공(self):
            self.service.repository.find_by_id = MagicMock(return_value=self.user)
            auth_data = AuthData(uid=1, access_token="valid-token")

            self.service.logout(auth_data)

            assert self.user.access_token is None

        def test_사용자_없음(self):
            self.service.repository.find_by_id = MagicMock(return_value=None)
            auth_data = AuthData(uid=999, access_token="any-token")

            with pytest.raises(UserNotFoundException):
                self.service.logout(auth_data)
