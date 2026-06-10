import os
from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from app.config.database import get_db
from app.domain.ums.jwt_util import create_token
from app.main import app


class TestUmsRouter:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    class TestJoin:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.service.UserRepository")
        def test_회원가입_성공(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = None
            mock_user = MagicMock()
            mock_user.id = 1
            mock_repo.save.return_value = mock_user
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/user/join", json={
                "email": "test@yologram.link",
                "name": "테스트",
                "nickname": "tester",
                "password": "password123!",
            })

            assert response.status_code == 201
            assert response.json() == {"data": {"uid": 1}}

        @patch("app.domain.ums.service.UserRepository")
        def test_이메일_중복_시_409(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_email.return_value = MagicMock()
            mock_repo_cls.return_value = mock_repo

            response = self.client.post("/api/v2/ums/user/join", json={
                "email": "duplicate@yologram.link",
                "name": "테스트",
                "nickname": "tester",
                "password": "password123!",
            })

            assert response.status_code == 409
            assert response.json()["errorCode"] == "USER_DUPLICATE"

        def test_입력값_검증_이메일_형식(self):
            response = self.client.post("/api/v2/ums/user/join", json={
                "email": "invalid-email",
                "name": "테스트",
                "nickname": "tester",
                "password": "password123!",
            })

            assert response.status_code == 422

        def test_입력값_검증_비밀번호_길이(self):
            response = self.client.post("/api/v2/ums/user/join", json={
                "email": "test@yologram.link",
                "name": "테스트",
                "nickname": "tester",
                "password": "short",
            })

            assert response.status_code == 422

    class TestGetMe:

        def setup_method(self):
            self.mock_db = MagicMock()
            app.dependency_overrides[get_db] = lambda: self.mock_db
            self.client = TestClient(app)
            self.token = create_token(1)

        def teardown_method(self):
            app.dependency_overrides.clear()

        @patch("app.domain.ums.service.UserRepository")
        def test_회원정보_조회_성공(self, mock_repo_cls):
            from app.domain.ums.enum import UserType
            from datetime import datetime

            mock_user = MagicMock()
            mock_user.id = 1
            mock_user.email = "test@yologram.link"
            mock_user.name = "테스트"
            mock_user.nickname = "tester"
            mock_user.avatar = None
            mock_user.type = UserType.DEFAULT
            mock_user.joined_date = datetime(2025, 1, 1)

            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = mock_user
            mock_repo_cls.return_value = mock_repo

            response = self.client.get(
                "/api/v2/ums/user/me",
                headers={"Authorization": f"Bearer {self.token}"},
            )

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["uid"] == 1
            assert data["email"] == "test@yologram.link"
            assert data["name"] == "테스트"
            assert data["nickname"] == "tester"
            assert data["avatar"] is None
            assert data["type"] == "DEFAULT"
            assert "joinedDate" in data

        @patch("app.domain.ums.service.UserRepository")
        def test_아바타가_있으면_포함(self, mock_repo_cls):
            from app.domain.ums.enum import UserType
            from datetime import datetime

            mock_user = MagicMock()
            mock_user.id = 1
            mock_user.email = "test@yologram.link"
            mock_user.name = "테스트"
            mock_user.nickname = "tester"
            mock_user.avatar = "https://example.com/avatar.png"
            mock_user.type = UserType.DEFAULT
            mock_user.joined_date = datetime(2025, 1, 1)

            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = mock_user
            mock_repo_cls.return_value = mock_repo

            response = self.client.get(
                "/api/v2/ums/user/me",
                headers={"Authorization": f"Bearer {self.token}"},
            )

            assert response.status_code == 200
            assert response.json()["data"]["avatar"] == "https://example.com/avatar.png"

        def test_인증_헤더_없으면_401(self):
            response = self.client.get("/api/v2/ums/user/me")

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_TOKEN_INVALID"

        def test_유효하지_않은_토큰이면_401(self):
            response = self.client.get(
                "/api/v2/ums/user/me",
                headers={"Authorization": "Bearer invalid-token"},
            )

            assert response.status_code == 401

        @patch("app.domain.ums.service.UserRepository")
        def test_존재하지_않는_유저면_404(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.get(
                "/api/v2/ums/user/me",
                headers={"Authorization": f"Bearer {self.token}"},
            )

            assert response.status_code == 404
            assert response.json()["errorCode"] == "USER_NOT_FOUND"
