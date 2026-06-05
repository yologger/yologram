from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient

from app.config.database import get_db
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
