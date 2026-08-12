import os
from unittest.mock import MagicMock, patch

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from fastapi.testclient import TestClient

from app.config.database import get_db
from app.core.exception import PostNotFoundException
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData
from app.main import app


class TestTechPostLikeRouter:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    def _authenticate(self, uid: int = 7):
        app.dependency_overrides[get_authenticated_user] = lambda: AuthData(uid=uid, access_token="t")

    @patch("app.domain.pms.tech.router.TechPostLikeService")
    def test_좋아요_시_200과_서비스_위임(self, mock_service_cls):
        self._authenticate(7)
        mock_service = MagicMock()
        mock_service_cls.return_value = mock_service

        response = self.client.post("/api/v2/pms/tech/posts/1/like")

        assert response.status_code == 200
        mock_service.like.assert_called_once_with(1, 7)

    def test_좋아요_미인증_시_401(self):
        response = self.client.post("/api/v2/pms/tech/posts/1/like")

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.pms.tech.router.TechPostLikeService")
    def test_없는_글에_좋아요하면_404(self, mock_service_cls):
        self._authenticate(7)
        mock_service = MagicMock()
        mock_service.like.side_effect = PostNotFoundException()
        mock_service_cls.return_value = mock_service

        response = self.client.post("/api/v2/pms/tech/posts/999/like")

        assert response.status_code == 404
        assert response.json()["errorCode"] == "POST_NOT_FOUND"

    @patch("app.domain.pms.tech.router.TechPostLikeService")
    def test_좋아요_취소_시_200과_서비스_위임(self, mock_service_cls):
        self._authenticate(7)
        mock_service = MagicMock()
        mock_service_cls.return_value = mock_service

        response = self.client.delete("/api/v2/pms/tech/posts/1/like")

        assert response.status_code == 200
        mock_service.unlike.assert_called_once_with(1, 7)

    def test_좋아요_취소_미인증_시_401(self):
        response = self.client.delete("/api/v2/pms/tech/posts/1/like")

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.pms.tech.router.TechPostLikeService")
    def test_없는_글의_좋아요를_취소하면_404(self, mock_service_cls):
        self._authenticate(7)
        mock_service = MagicMock()
        mock_service.unlike.side_effect = PostNotFoundException()
        mock_service_cls.return_value = mock_service

        response = self.client.delete("/api/v2/pms/tech/posts/999/like")

        assert response.status_code == 404
        assert response.json()["errorCode"] == "POST_NOT_FOUND"
