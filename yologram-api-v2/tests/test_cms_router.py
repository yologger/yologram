import os
from unittest.mock import MagicMock, patch

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from fastapi.testclient import TestClient

from app.config.database import get_db
from app.domain.cms.enum import Section
from app.domain.cms.model import PostCategory
from app.main import app


class TestCmsRouter:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    @patch("app.domain.cms.service.PostCategoryRepository")
    def test_카테고리_목록_조회_200(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active_by_section.return_value = [
            PostCategory(id=1, section=Section.TECH, name="Frontend", sort_order=1, is_active=True),
            PostCategory(id=2, section=Section.TECH, name="Backend", sort_order=2, is_active=True),
        ]
        mock_repo_cls.return_value = mock_repo

        response = self.client.get("/api/v2/cms/tech/categories")

        assert response.status_code == 200
        body = response.json()
        assert body["data"][0]["id"] == 1
        assert body["data"][0]["name"] == "Frontend"
        assert body["data"][0]["sortOrder"] == 1
        assert body["data"][1]["name"] == "Backend"

    @patch("app.domain.cms.service.PostCategoryRepository")
    def test_카테고리가_없으면_빈_배열_반환(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active_by_section.return_value = []
        mock_repo_cls.return_value = mock_repo

        response = self.client.get("/api/v2/cms/politics/categories")

        assert response.status_code == 200
        assert response.json()["data"] == []

    def test_유효하지_않은_section이면_400(self):
        response = self.client.get("/api/v2/cms/unknown/categories")

        assert response.status_code == 400
        body = response.json()
        assert body["errorCode"] == "INVALID_SECTION"
        assert body["errorMessage"]
