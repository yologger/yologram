import os
from unittest.mock import MagicMock, patch

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from fastapi.testclient import TestClient

from app.config.database import get_db
from app.domain.cms.tech.model import TechCategory
from app.main import app


class TestTechCategoryRouter:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    @patch("app.domain.cms.tech.service.TechCategoryRepository")
    def test_카테고리_목록_조회_200(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active.return_value = [
            TechCategory(id=1, name="Frontend", sort_order=1, is_active=True),
            TechCategory(id=2, name="Backend", sort_order=2, is_active=True),
        ]
        mock_repo_cls.return_value = mock_repo

        response = self.client.get("/api/v2/cms/tech/categories")

        assert response.status_code == 200
        body = response.json()
        assert body["data"][0]["id"] == 1
        assert body["data"][0]["name"] == "Frontend"
        assert body["data"][0]["sortOrder"] == 1
        assert body["data"][1]["name"] == "Backend"

    @patch("app.domain.cms.tech.service.TechCategoryRepository")
    def test_카테고리가_없으면_빈_배열_반환(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active.return_value = []
        mock_repo_cls.return_value = mock_repo

        response = self.client.get("/api/v2/cms/tech/categories")

        assert response.status_code == 200
        assert response.json()["data"] == []

    def test_다른_section_경로면_404(self):
        # 테이블 분리로 경로가 tech 고정 — 다른 섹션 경로는 라우트가 없어 404
        response = self.client.get("/api/v2/cms/unknown/categories")

        assert response.status_code == 404
        body = response.json()
        assert body["errorCode"] == "NOT_FOUND"
        assert body["errorMessage"]
