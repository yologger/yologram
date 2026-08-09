import os
from datetime import datetime
from unittest.mock import MagicMock, patch

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from fastapi.testclient import TestClient

from app.config.database import get_db
from app.domain.news.tech.cursor import TechNewsCursor
from app.domain.news.tech.model import TechNews, TechNewsCategoryMapping, TechNewsStatus
from app.infra.cache.tech_news_first_page_cache import TechNewsFirstPageCache
from app.main import app


def _news(news_id: int = 1) -> TechNews:
    return TechNews(
        id=news_id,
        source_id=1,
        title="코틀린 코루틴 딥다이브",
        summary="**📌 한 줄 요약** 코루틴 내부 구조 해설.",
        link=f"https://tech.example.com/posts/{news_id}",
        source_name="테크 블로그",
        published_at=datetime(2026, 7, 18, 9, 0),
        status=TechNewsStatus.SUMMARIZED.value,
    )


class TestTechNewsRouter:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)
        # 첫 페이지 캐시는 mock CacheService(전체 미스)로 대체 — 실 Redis 접근 차단, DB 경로 그대로 검증
        self.cache_service = MagicMock()
        self.cache_service.get_or_null.return_value = None
        self.cache_patcher = patch(
            "app.domain.news.tech.service.TechNewsFirstPageCache",
            return_value=TechNewsFirstPageCache(self.cache_service),
        )
        self.cache_patcher.start()

    def teardown_method(self):
        self.cache_patcher.stop()
        app.dependency_overrides.clear()

    @patch("app.domain.news.tech.service.LocalCmsApiClient")
    @patch("app.domain.news.tech.service.TechNewsCategoryMappingRepository")
    @patch("app.domain.news.tech.service.TechNewsRepository")
    def test_200과_뉴스_목록을_반환한다(self, mock_repo_cls, mock_mapping_cls, mock_category_cls):
        mock_repo = MagicMock()
        mock_repo.find_summarized_news.return_value = [_news(1)]
        mock_repo_cls.return_value = mock_repo
        mock_mapping = MagicMock()
        mock_mapping.find_by_news_ids.return_value = [
            TechNewsCategoryMapping(id=1, news_id=1, category_id=2),
            TechNewsCategoryMapping(id=2, news_id=1, category_id=4),
        ]
        mock_mapping_cls.return_value = mock_mapping
        mock_category = MagicMock()
        mock_category.find_category_names.return_value = {2: "Backend", 4: "DevOps"}
        mock_category_cls.return_value = mock_category

        response = self.client.get("/api/v2/news/tech")

        assert response.status_code == 200
        body = response.json()
        assert body["data"][0]["id"] == 1
        assert body["data"][0]["title"] == "코틀린 코루틴 딥다이브"
        assert body["data"][0]["summary"] == "**📌 한 줄 요약** 코루틴 내부 구조 해설."
        assert body["data"][0]["sourceName"] == "테크 블로그"
        assert body["data"][0]["categories"] == ["Backend", "DevOps"]  # tech_category 라벨 해석
        assert body["nextCursor"] == TechNewsCursor.encode(datetime(2026, 7, 18, 9, 0), 1)

    @patch("app.domain.news.tech.service.LocalCmsApiClient")
    @patch("app.domain.news.tech.service.TechNewsCategoryMappingRepository")
    @patch("app.domain.news.tech.service.TechNewsRepository")
    def test_cursor_size_파라미터가_리포지토리까지_전달된다(self, mock_repo_cls, mock_mapping_cls, mock_category_cls):
        mock_repo = MagicMock()
        mock_repo.find_summarized_news.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_mapping_cls.return_value = MagicMock(find_by_news_ids=MagicMock(return_value=[]))
        mock_category_cls.return_value = MagicMock(find_category_names=MagicMock(return_value={}))
        cursor = TechNewsCursor.encode(datetime(2026, 7, 18, 9, 0), 42)

        response = self.client.get(f"/api/v2/news/tech?cursor={cursor}&size=10")

        assert response.status_code == 200
        mock_repo.find_summarized_news.assert_called_once_with(
            None, TechNewsCursor(published_at=datetime(2026, 7, 18, 9, 0), id=42), 10
        )

    @patch("app.domain.news.tech.service.LocalCmsApiClient")
    @patch("app.domain.news.tech.service.TechNewsCategoryMappingRepository")
    @patch("app.domain.news.tech.service.TechNewsRepository")
    def test_categoryId_파라미터가_리포지토리까지_전달된다(self, mock_repo_cls, mock_mapping_cls, mock_category_cls):
        mock_repo = MagicMock()
        mock_repo.find_summarized_news.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_mapping_cls.return_value = MagicMock(find_by_news_ids=MagicMock(return_value=[]))
        mock_category_cls.return_value = MagicMock(find_category_names=MagicMock(return_value={}))

        response = self.client.get("/api/v2/news/tech?categoryId=2")

        assert response.status_code == 200
        mock_repo.find_summarized_news.assert_called_once_with(2, None, 20)

    def test_categoryId가_숫자가_아니면_400_VALIDATION_ERROR를_반환한다(self):
        response = self.client.get("/api/v2/news/tech?categoryId=Backend")

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    @patch("app.domain.news.tech.service.LocalCmsApiClient")
    @patch("app.domain.news.tech.service.TechNewsCategoryMappingRepository")
    @patch("app.domain.news.tech.service.TechNewsRepository")
    def test_결과가_비면_nextCursor_필드가_생략된다(self, mock_repo_cls, mock_mapping_cls, mock_category_cls):
        mock_repo_cls.return_value = MagicMock(find_summarized_news=MagicMock(return_value=[]))
        mock_mapping_cls.return_value = MagicMock(find_by_news_ids=MagicMock(return_value=[]))
        mock_category_cls.return_value = MagicMock(find_category_names=MagicMock(return_value={}))

        response = self.client.get("/api/v2/news/tech")

        assert response.status_code == 200
        assert response.json() == {"data": []}  # nextCursor 생략 (api-v1 NON_NULL과 동일)

    def test_잘못된_커서면_400_INVALID_CURSOR를_반환한다(self):
        response = self.client.get("/api/v2/news/tech?cursor=broken")

        assert response.status_code == 400
        assert response.json()["errorCode"] == "INVALID_CURSOR"
        assert response.json()["errorMessage"] == "유효하지 않은 커서입니다."
