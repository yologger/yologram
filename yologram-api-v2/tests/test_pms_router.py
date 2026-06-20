import os
from datetime import datetime
from unittest.mock import MagicMock, patch

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from fastapi.testclient import TestClient

from app.config.database import get_db
from app.domain.pms.model import Post, PostCategoryMapping
from app.domain.cms.enum import Section
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData
from app.main import app


def _saved_post(post_id: int = 10) -> Post:
    post = Post(section=Section.TECH, user_id=1, content="내용")
    post.id = post_id
    return post


class TestPmsRouter:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    def _authenticate(self):
        app.dependency_overrides[get_authenticated_user] = lambda: AuthData(uid=1, access_token="t")

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_정상_작성_시_201(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        self._authenticate()
        mock_post_repo = MagicMock()
        mock_post_repo.save.return_value = _saved_post(10)
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_client = MagicMock()
        mock_client.all_active_in_section.return_value = True
        mock_client_cls.return_value = mock_client

        response = self.client.post(
            "/api/v2/pms/tech/posts",
            json={"title": "첫 글", "content": "내용입니다", "categoryIds": [1]},
        )

        assert response.status_code == 201
        assert response.json() == {"data": {"id": 10}}

    def test_미인증_시_401(self):
        response = self.client.post("/api/v2/pms/tech/posts", json={"content": "내용"})

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_카테고리가_해당_section_것이_아니면_400(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        self._authenticate()
        mock_post_repo_cls.return_value = MagicMock()
        mock_pc_repo_cls.return_value = MagicMock()
        mock_client = MagicMock()
        mock_client.all_active_in_section.return_value = False
        mock_client_cls.return_value = mock_client

        response = self.client.post(
            "/api/v2/pms/tech/posts",
            json={"content": "내용", "categoryIds": [99]},
        )

        assert response.status_code == 400
        assert response.json()["errorCode"] == "INVALID_POST_CATEGORY"

    def test_유효하지_않은_section이면_400(self):
        self._authenticate()

        response = self.client.post("/api/v2/pms/unknown/posts", json={"content": "내용", "categoryIds": [1]})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "INVALID_SECTION"

    def test_내용_누락_시_400(self):
        self._authenticate()

        response = self.client.post("/api/v2/pms/tech/posts", json={"categoryIds": [1]})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    def test_카테고리_미선택_시_400(self):
        self._authenticate()

        response = self.client.post("/api/v2/pms/tech/posts", json={"content": "내용", "categoryIds": []})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    def test_카테고리_4개_이상이면_400(self):
        self._authenticate()

        response = self.client.post(
            "/api/v2/pms/tech/posts",
            json={"content": "내용", "categoryIds": [1, 2, 3, 4]},
        )

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_상세_조회_시_200(self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls):
        post = _saved_post(1)
        post.user_id = 12
        post.title = "제목"
        post.like_count = 0
        post.comment_count = 0
        post.created_at = datetime(2026, 1, 1, 0, 0)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_id.return_value = [PostCategoryMapping(post_id=1, category_id=1)]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nickname.return_value = "tester"
        mock_user_cls.return_value = mock_user

        response = self.client.get("/api/v2/pms/tech/posts/1")

        assert response.status_code == 200
        body = response.json()["data"]
        assert body["id"] == 1
        assert body["author"]["nickname"] == "tester"
        assert body["content"] == "내용"
        assert body["categoryIds"] == [1]

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_존재하지_않는_게시글이면_404(self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo

        response = self.client.get("/api/v2/pms/tech/posts/99")

        assert response.status_code == 404
        assert response.json()["errorCode"] == "POST_NOT_FOUND"

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_목록_조회_시_200과_data_nextCursor(self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls):
        post = _saved_post(2)
        post.user_id = 12
        post.title = "제목"
        post.like_count = 0
        post.comment_count = 0
        post.created_at = datetime(2026, 1, 1, 0, 0)
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts_by_section.return_value = [post]
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = [PostCategoryMapping(post_id=2, category_id=1)]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {12: "tester"}
        mock_user_cls.return_value = mock_user

        response = self.client.get("/api/v2/pms/tech/posts?size=5")

        assert response.status_code == 200
        body = response.json()
        assert body["data"][0]["id"] == 2
        assert body["data"][0]["author"]["nickname"] == "tester"
        assert body["data"][0]["categoryIds"] == [1]
        assert body["nextCursor"] is not None

    def test_목록_조회_시_유효하지_않은_section이면_400(self):
        response = self.client.get("/api/v2/pms/unknown/posts")

        assert response.status_code == 400
        assert response.json()["errorCode"] == "INVALID_SECTION"
