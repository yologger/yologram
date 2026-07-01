import os
from unittest.mock import MagicMock, patch

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from fastapi.testclient import TestClient

from app.config.database import get_db
from app.domain.comment.model import Comment
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData
from app.main import app


def _saved_comment(comment_id: int = 10) -> Comment:
    comment = Comment(post_id=1, user_id=1, content="내용")
    comment.id = comment_id
    return comment


class TestCommentRouter:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    def _authenticate(self):
        app.dependency_overrides[get_authenticated_user] = lambda: AuthData(uid=1, access_token="t")

    @patch("app.domain.comment.service.LocalPostQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_정상_작성_시_201(self, mock_comment_repo_cls, mock_client_cls):
        self._authenticate()
        mock_comment_repo = MagicMock()
        mock_comment_repo.save.return_value = _saved_comment(10)
        mock_comment_repo_cls.return_value = mock_comment_repo
        mock_client = MagicMock()
        mock_client.exists.return_value = True
        mock_client_cls.return_value = mock_client

        response = self.client.post("/api/v2/comments/posts/1", json={"content": "좋은 글 감사합니다"})

        assert response.status_code == 201
        assert response.json() == {"data": {"id": 10}}

    def test_미인증_시_401(self):
        response = self.client.post("/api/v2/comments/posts/1", json={"content": "내용"})

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.comment.service.LocalPostQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_대상_글이_없으면_404(self, mock_comment_repo_cls, mock_client_cls):
        self._authenticate()
        mock_comment_repo_cls.return_value = MagicMock()
        mock_client = MagicMock()
        mock_client.exists.return_value = False
        mock_client_cls.return_value = mock_client

        response = self.client.post("/api/v2/comments/posts/99", json={"content": "내용"})

        assert response.status_code == 404
        assert response.json()["errorCode"] == "POST_NOT_FOUND"

    def test_내용_누락_시_400(self):
        self._authenticate()

        response = self.client.post("/api/v2/comments/posts/1", json={})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    def test_내용_빈값_시_400(self):
        self._authenticate()

        response = self.client.post("/api/v2/comments/posts/1", json={"content": "   "})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    def test_내용_1000자_초과_시_400(self):
        self._authenticate()

        response = self.client.post("/api/v2/comments/posts/1", json={"content": "가" * 1001})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"
