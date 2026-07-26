import os
import time
from unittest.mock import MagicMock, patch

import bcrypt
from fastapi.testclient import TestClient

os.environ["JWT_SECRET"] = "test-jwt-secret-key-for-testing"
os.environ["ADMIN_JWT_SECRET"] = "test-admin-jwt-secret-key-for-testing"

from app.config.database import get_db
from app.config.settings import get_settings
from app.domain.ums.admin_jwt_util import create_admin_token
from app.domain.ums.jwt_util import create_token
from app.domain.ums.model import AdminUser
from app.main import app


# lru_cache 초기화
get_settings.cache_clear()


class TestAdminRouter:

    class TestCreate:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)
            self.token = create_admin_token(1)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.admin_service.AdminUserRepository")
        def test_어드민_생성_성공_201(self, mock_repo_cls):
            saved = AdminUser(email="new@yologram.link", name="새어드민", password="hashed")
            saved.id = 2
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = None
            mock_repo.save.return_value = saved
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"email": "new@yologram.link", "name": "새어드민", "password": "password123!"},
            )

            assert response.status_code == 201
            assert response.json()["data"]["uid"] == 2

        @patch("app.domain.ums.admin_service.AdminUserRepository")
        def test_중복_이메일_409(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = MagicMock()
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"email": "dup@yologram.link", "name": "어드민", "password": "password123!"},
            )

            assert response.status_code == 409
            assert response.json()["errorCode"] == "ADMIN_USER_DUPLICATE"

        def test_이메일_형식_오류_400(self):
            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"email": "invalid", "name": "어드민", "password": "password123!"},
            )

            assert response.status_code == 400

        def test_이름_1자_400(self):
            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"email": "new@yologram.link", "name": "a", "password": "password123!"},
            )

            assert response.status_code == 400

        def test_이름_21자_400(self):
            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"email": "new@yologram.link", "name": "a" * 21, "password": "password123!"},
            )

            assert response.status_code == 400

        def test_비밀번호_7자_400(self):
            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"email": "new@yologram.link", "name": "어드민", "password": "a" * 7},
            )

            assert response.status_code == 400

        def test_비밀번호_21자_400(self):
            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                headers={"Authorization": f"Bearer {self.token}"},
                json={"email": "new@yologram.link", "name": "어드민", "password": "a" * 21},
            )

            assert response.status_code == 400

        def test_토큰_없음_401(self):
            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                json={"email": "new@yologram.link", "name": "어드민", "password": "password123!"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

        def test_유저_토큰_401(self):
            user_token = create_token(1)

            response = self.client.post(
                "/api/v2/ums/admin/admin-users",
                headers={"Authorization": f"Bearer {user_token}"},
                json={"email": "new@yologram.link", "name": "어드민", "password": "password123!"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    class TestLogin:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)
            self.hashed_pw = bcrypt.hashpw("password123!".encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.admin_service.AdminUserRepository")
        def test_로그인_성공(self, mock_repo_cls):
            admin = AdminUser(email="admin@yologram.link", name="어드민", password=self.hashed_pw)
            admin.id = 1
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = admin
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/admin/auth/login", json={
                "email": "admin@yologram.link",
                "password": "password123!",
            })

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["uid"] == 1
            assert data["email"] == "admin@yologram.link"
            assert data["name"] == "어드민"
            assert "accessToken" in data

        @patch("app.domain.ums.admin_service.AdminUserRepository")
        def test_존재하지_않는_어드민_404(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/admin/auth/login", json={
                "email": "notfound@yologram.link",
                "password": "password123!",
            })

            assert response.status_code == 404
            assert response.json()["errorCode"] == "ADMIN_USER_NOT_FOUND"

        @patch("app.domain.ums.admin_service.AdminUserRepository")
        def test_비밀번호_불일치_401(self, mock_repo_cls):
            admin = AdminUser(email="admin@yologram.link", name="어드민", password=self.hashed_pw)
            admin.id = 1
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = admin
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/admin/auth/login", json={
                "email": "admin@yologram.link",
                "password": "wrongpassword",
            })

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_WRONG_PASSWORD"

        def test_이메일_형식_오류_400(self):
            response = self.client.post("/api/v2/ums/admin/auth/login", json={
                "email": "invalid",
                "password": "password123!",
            })

            assert response.status_code == 400

    class TestValidateToken:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.admin_service.AdminUserRepository")
        def test_토큰_검증_성공(self, mock_repo_cls):
            token = create_admin_token(1)
            admin = AdminUser(email="admin@yologram.link", name="어드민", password="hashed")
            admin.id = 1
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = admin
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/admin/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["uid"] == 1
            assert data["email"] == "admin@yologram.link"
            assert data["name"] == "어드민"

        @patch("app.domain.ums.admin_service.AdminUserRepository")
        def test_어드민_없음_404(self, mock_repo_cls):
            token = create_admin_token(999)
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                "/api/v2/ums/admin/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 404
            assert response.json()["errorCode"] == "ADMIN_USER_NOT_FOUND"

        def test_헤더_없음_401(self):
            response = self.client.post("/api/v2/ums/admin/auth/validate-token")

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

        def test_빈_Bearer_401(self):
            response = self.client.post(
                "/api/v2/ums/admin/auth/validate-token",
                headers={"Authorization": "Bearer "},
            )

            assert response.status_code == 401

        def test_위조된_토큰_401(self):
            import jwt as pyjwt

            settings = get_settings()
            payload = {
                "uid": 1,
                "iss": settings.admin_jwt_issuer,
                "aud": settings.admin_jwt_audience,
                "iat": time.time(),
                "exp": time.time() + 3600,
            }
            forged = pyjwt.encode(payload, "wrong-secret", algorithm="HS256")

            response = self.client.post(
                "/api/v2/ums/admin/auth/validate-token",
                headers={"Authorization": f"Bearer {forged}"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

        def test_만료된_토큰_401(self):
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

            response = self.client.post(
                "/api/v2/ums/admin/auth/validate-token",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_EXPIRED_TOKEN"

        def test_유저_토큰_401(self):
            user_token = create_token(1)

            response = self.client.post(
                "/api/v2/ums/admin/auth/validate-token",
                headers={"Authorization": f"Bearer {user_token}"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    class TestLogout:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.admin_service.AdminUserRepository")
        def test_로그아웃_성공_204(self, mock_repo_cls):
            token = create_admin_token(1)
            mock_repo_cls.return_value = MagicMock()

            response = self.client.post(
                "/api/v2/ums/admin/auth/logout",
                headers={"Authorization": f"Bearer {token}"},
            )

            assert response.status_code == 204

        def test_헤더_없음_401(self):
            response = self.client.post("/api/v2/ums/admin/auth/logout")

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"
