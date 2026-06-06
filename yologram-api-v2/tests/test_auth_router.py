import os
from unittest.mock import MagicMock, patch

import bcrypt
import pytest
from fastapi.testclient import TestClient

os.environ["JWT_SECRET"] = "test-jwt-secret-key-for-testing"

from app.config.database import get_db
from app.domain.ums.jwt_util import create_token
from app.domain.ums.model import User
from app.main import app


class TestAuthRouter:

    class TestLogin:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)
            self.hashed_pw = bcrypt.hashpw("password123!".encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_로그인_성공(self, mock_repo_cls):
            user = User(email="test@yologram.link", name="테스터", nickname="tester", password=self.hashed_pw)
            user.id = 1
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/login", json={
                "email": "test@yologram.link",
                "password": "password123!",
            })

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["uid"] == 1
            assert data["email"] == "test@yologram.link"
            assert data["name"] == "테스터"
            assert data["nickname"] == "tester"
            assert "accessToken" in data

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_존재하지_않는_사용자_404(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/login", json={
                "email": "notfound@yologram.link",
                "password": "password123!",
            })

            assert response.status_code == 404
            assert response.json()["errorCode"] == "USER_NOT_FOUND"

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_비밀번호_불일치_401(self, mock_repo_cls):
            user = User(email="test@yologram.link", name="테스터", nickname="tester", password=self.hashed_pw)
            user.id = 1
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/auth/login", json={
                "email": "test@yologram.link",
                "password": "wrongpassword",
            })

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_WRONG_PASSWORD"

        def test_이메일_형식_오류_422(self):
            response = self.client.post("/api/v2/ums/auth/login", json={
                "email": "invalid",
                "password": "password123!",
            })

            assert response.status_code == 422

    class TestValidateToken:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_토큰_검증_성공(self, mock_repo_cls):
            token = create_token(1)
            user = User(email="test@yologram.link", name="테스터", nickname="tester", password="hashed")
            user.id = 1
            user.access_token = token
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["uid"] == 1
            assert data["email"] == "test@yologram.link"

        def test_헤더_없음_401(self):
            response = self.client.post("/api/v2/ums/auth/validate-token")

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_TOKEN_INVALID"

        def test_빈_Bearer_401(self):
            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": "Bearer "},
            )

            assert response.status_code == 401

        def test_Basic_스킴_401(self):
            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": "Basic abc123"},
            )

            assert response.status_code == 401

        def test_만료된_토큰_401(self):
            import time
            import jwt as pyjwt
            from app.config.settings import get_settings

            settings = get_settings()
            payload = {
                "uid": 1,
                "iss": settings.jwt_issuer,
                "aud": settings.jwt_audience,
                "iat": time.time() - 100,
                "exp": time.time() - 10,
            }
            token = pyjwt.encode(payload, settings.jwt_secret, algorithm="HS256")

            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_TOKEN_EXPIRED"

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_DB_토큰_불일치_401(self, mock_repo_cls):
            token = create_token(1)
            user = User(email="test@yologram.link", name="테스터", nickname="tester", password="hashed")
            user.id = 1
            user.access_token = "different-token"
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_TOKEN_INVALID"

            assert response.status_code == 401

    class TestLogout:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.auth_service.UserRepository")
        def test_로그아웃_성공_204(self, mock_repo_cls):
            token = create_token(1)
            user = User(email="test@yologram.link", name="테스터", nickname="tester", password="hashed")
            user.id = 1
            user.access_token = token
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/auth/logout",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 204

        def test_헤더_없음_401(self):
            response = self.client.post("/api/v2/ums/auth/logout")

            assert response.status_code == 401
