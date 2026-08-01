import os
from unittest.mock import MagicMock, patch

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from fastapi.testclient import TestClient

from app.config.database import get_db
from app.domain.comment.tech.model import TechPostComment
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData
from app.main import app

NEW_POSTS_PATH = "/api/v2/comments/tech/posts"
NEW_COMMENT_PATH = "/api/v2/comments/tech"


def _saved_comment(comment_id: int = 10) -> TechPostComment:
    comment = TechPostComment(post_id=1, user_id=1, content="내용")
    comment.id = comment_id
    return comment


def _comment(comment_id: int, post_id: int = 1, user_id: int = 1) -> TechPostComment:
    from datetime import datetime

    comment = TechPostComment(post_id=post_id, user_id=user_id, content="내용")
    comment.id = comment_id
    comment.created_at = datetime(2026, 1, 1, 0, 0, 0)
    return comment


class _RouterTestBase:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    def _authenticate(self, uid: int = 1):
        app.dependency_overrides[get_authenticated_user] = lambda: AuthData(uid=uid, access_token="t")


class _CreateCommentCases(_RouterTestBase):
    POSTS_PATH: str

    @patch("app.domain.comment.tech.service.LocalTechPostQueryClient")
    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_정상_작성_시_201(self, mock_comment_repo_cls, mock_client_cls):
        self._authenticate()
        mock_comment_repo = MagicMock()
        mock_comment_repo.save.return_value = _saved_comment(10)
        mock_comment_repo_cls.return_value = mock_comment_repo
        mock_client = MagicMock()
        mock_client.exists.return_value = True
        mock_client_cls.return_value = mock_client

        response = self.client.post(f"{self.POSTS_PATH}/1", json={"content": "좋은 글 감사합니다"})

        assert response.status_code == 201
        assert response.json() == {"data": {"id": 10}}

    def test_미인증_시_401(self):
        response = self.client.post(f"{self.POSTS_PATH}/1", json={"content": "내용"})

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.comment.tech.service.LocalTechPostQueryClient")
    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_대상_글이_없으면_404(self, mock_comment_repo_cls, mock_client_cls):
        self._authenticate()
        mock_comment_repo_cls.return_value = MagicMock()
        mock_client = MagicMock()
        mock_client.exists.return_value = False
        mock_client_cls.return_value = mock_client

        response = self.client.post(f"{self.POSTS_PATH}/99", json={"content": "내용"})

        assert response.status_code == 404
        assert response.json()["errorCode"] == "POST_NOT_FOUND"

    def test_내용_누락_시_400(self):
        self._authenticate()

        response = self.client.post(f"{self.POSTS_PATH}/1", json={})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    def test_내용_빈값_시_400(self):
        self._authenticate()

        response = self.client.post(f"{self.POSTS_PATH}/1", json={"content": "   "})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    def test_내용_1000자_초과_시_400(self):
        self._authenticate()

        response = self.client.post(f"{self.POSTS_PATH}/1", json={"content": "가" * 1001})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"


class TestTechCommentCreateRouter(_CreateCommentCases):
    POSTS_PATH = NEW_POSTS_PATH


class _UpdateCommentCases(_RouterTestBase):
    COMMENT_PATH: str

    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_정상_수정_시_204(self, mock_repo_cls):
        self._authenticate(uid=1)
        mock_repo = MagicMock()
        mock_repo.find_by_id.return_value = _saved_comment(10)  # user_id=1
        mock_repo_cls.return_value = mock_repo

        response = self.client.patch(f"{self.COMMENT_PATH}/10", json={"content": "수정된 내용"})

        assert response.status_code == 204
        assert response.content == b""

    def test_미인증_시_401(self):
        response = self.client.patch(f"{self.COMMENT_PATH}/10", json={"content": "수정된 내용"})

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_없는_댓글이면_404(self, mock_repo_cls):
        self._authenticate(uid=1)
        mock_repo = MagicMock()
        mock_repo.find_by_id.return_value = None
        mock_repo_cls.return_value = mock_repo

        response = self.client.patch(f"{self.COMMENT_PATH}/99", json={"content": "수정된 내용"})

        assert response.status_code == 404
        assert response.json()["errorCode"] == "COMMENT_NOT_FOUND"

    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_본인_댓글_아니면_403(self, mock_repo_cls):
        self._authenticate(uid=2)
        mock_repo = MagicMock()
        mock_repo.find_by_id.return_value = _saved_comment(10)  # user_id=1
        mock_repo_cls.return_value = mock_repo

        response = self.client.patch(f"{self.COMMENT_PATH}/10", json={"content": "수정된 내용"})

        assert response.status_code == 403
        assert response.json()["errorCode"] == "COMMENT_FORBIDDEN"

    def test_내용_누락_시_400(self):
        self._authenticate()

        response = self.client.patch(f"{self.COMMENT_PATH}/10", json={})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    def test_내용_빈값_시_400(self):
        self._authenticate()

        response = self.client.patch(f"{self.COMMENT_PATH}/10", json={"content": "   "})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    def test_내용_1000자_초과_시_400(self):
        self._authenticate()

        response = self.client.patch(f"{self.COMMENT_PATH}/10", json={"content": "가" * 1001})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"


class TestTechCommentUpdateRouter(_UpdateCommentCases):
    COMMENT_PATH = NEW_COMMENT_PATH


class _DeleteCommentCases(_RouterTestBase):
    COMMENT_PATH: str

    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_정상_삭제_시_204(self, mock_repo_cls):
        self._authenticate(uid=1)
        mock_repo = MagicMock()
        mock_repo.find_by_id.return_value = _saved_comment(10)  # user_id=1
        mock_repo_cls.return_value = mock_repo

        response = self.client.delete(f"{self.COMMENT_PATH}/10")

        assert response.status_code == 204
        assert response.content == b""
        mock_repo.delete.assert_called_once()

    def test_미인증_시_401(self):
        response = self.client.delete(f"{self.COMMENT_PATH}/10")

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_없는_댓글이면_404(self, mock_repo_cls):
        self._authenticate(uid=1)
        mock_repo = MagicMock()
        mock_repo.find_by_id.return_value = None
        mock_repo_cls.return_value = mock_repo

        response = self.client.delete(f"{self.COMMENT_PATH}/99")

        assert response.status_code == 404
        assert response.json()["errorCode"] == "COMMENT_NOT_FOUND"

    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_본인_댓글_아니면_403(self, mock_repo_cls):
        self._authenticate(uid=2)
        mock_repo = MagicMock()
        mock_repo.find_by_id.return_value = _saved_comment(10)  # user_id=1
        mock_repo_cls.return_value = mock_repo

        response = self.client.delete(f"{self.COMMENT_PATH}/10")

        assert response.status_code == 403
        assert response.json()["errorCode"] == "COMMENT_FORBIDDEN"


class TestTechCommentDeleteRouter(_DeleteCommentCases):
    COMMENT_PATH = NEW_COMMENT_PATH


class _QueryCommentCases(_RouterTestBase):
    POSTS_PATH: str

    @patch("app.domain.comment.tech.service.LocalUserQueryClient")
    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_조회는_공개_인증_불필요_200(self, mock_repo_cls, mock_user_cls):
        # 인증 오버라이드 없이 호출해도 성공해야 한다(공개 엔드포인트)
        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = [_comment(2), _comment(1)]
        mock_repo_cls.return_value = mock_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {1: "닉네임"}
        mock_user_cls.return_value = mock_user

        response = self.client.get(f"{self.POSTS_PATH}/1")

        assert response.status_code == 200
        body = response.json()
        assert len(body["data"]) == 2
        first = body["data"][0]
        assert first["id"] == 2
        assert first["postId"] == 1
        assert first["author"] == {"uid": 1, "nickname": "닉네임"}
        assert first["content"] == "내용"
        assert "createdAt" in first

    @patch("app.domain.comment.tech.service.LocalUserQueryClient")
    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_정상_조회_시_nextCursor는_마지막_id_인코딩(self, mock_repo_cls, mock_user_cls):
        import base64

        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = [_comment(5), _comment(3)]
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        response = self.client.get(f"{self.POSTS_PATH}/1")

        assert response.status_code == 200
        expected = base64.urlsafe_b64encode(b"3").decode().rstrip("=")
        assert response.json()["nextCursor"] == expected

    @patch("app.domain.comment.tech.service.LocalUserQueryClient")
    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_빈_목록이면_nextCursor_생략(self, mock_repo_cls, mock_user_cls):
        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        response = self.client.get(f"{self.POSTS_PATH}/999")

        assert response.status_code == 200
        # api-v1(@JsonInclude NON_NULL) 정합 — 커서 없으면 필드 자체 생략
        assert response.json() == {"data": []}

    @patch("app.domain.comment.tech.service.LocalUserQueryClient")
    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_cursor_전달_시_디코딩되어_repository로_전달(self, mock_repo_cls, mock_user_cls):
        import base64

        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        cursor = base64.urlsafe_b64encode(b"10").decode().rstrip("=")
        response = self.client.get(f"{self.POSTS_PATH}/1?cursor={cursor}")

        assert response.status_code == 200
        args = mock_repo.find_by_post_cursor.call_args[0]
        # (post_id, sort, cursor_id, limit)
        assert args[2] == 10

    @patch("app.domain.comment.tech.service.LocalUserQueryClient")
    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_sort_oldest_시_OLDEST로_조회(self, mock_repo_cls, mock_user_cls):
        from app.domain.comment.tech.sort import CommentSort

        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        response = self.client.get(f"{self.POSTS_PATH}/1?sort=oldest")

        assert response.status_code == 200
        args = mock_repo.find_by_post_cursor.call_args[0]
        assert args[1] == CommentSort.OLDEST

    @patch("app.domain.comment.tech.service.LocalUserQueryClient")
    @patch("app.domain.comment.tech.service.TechPostCommentRepository")
    def test_size_최대_50으로_제한(self, mock_repo_cls, mock_user_cls):
        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        response = self.client.get(f"{self.POSTS_PATH}/1?size=100")

        assert response.status_code == 200
        args = mock_repo.find_by_post_cursor.call_args[0]
        # (post_id, sort, cursor_id, limit)
        assert args[3] == 50

    def test_잘못된_커서면_400(self):
        response = self.client.get(f"{self.POSTS_PATH}/1?cursor=!!!invalid!!!")

        assert response.status_code == 400
        assert response.json()["errorCode"] == "INVALID_CURSOR"


class TestTechCommentQueryRouter(_QueryCommentCases):
    POSTS_PATH = NEW_POSTS_PATH
