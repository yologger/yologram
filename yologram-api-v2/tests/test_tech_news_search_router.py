import os
from unittest.mock import MagicMock, patch

from fastapi.testclient import TestClient

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")
os.environ.setdefault("ADMIN_JWT_SECRET", "test-admin-jwt-secret-key-for-testing")

from app.config.database import get_db
from app.config.settings import get_settings
from app.core.exception import (
    BlankSearchKeywordException,
    SearchPageTooDeepException,
    SearchUnavailableException,
)
from app.core.response import ApiEnvelopPage
from app.domain.news.tech.schema import TechNewsResponse
from app.domain.search.tech.model import TechSearchSort
from app.main import app

get_settings.cache_clear()

BASE_URL = "/api/v2/search/tech/news"
PATCH_SERVICE = "app.domain.search.tech.news_search_router.TechNewsSearchService"


def _page(items: list[TechNewsResponse] | None = None) -> ApiEnvelopPage:
    data = items or []
    return ApiEnvelopPage(
        data=data, page=0, size=10, total_pages=1, total_count=len(data), first=True, last=True
    )


def _news(news_id: int = 900) -> TechNewsResponse:
    return TechNewsResponse(
        id=news_id,
        title="제목",
        summary="요약",
        link=f"https://news.test/{news_id}",
        source_name="GeekNews",
        categories=["인프라"],
        published_at="2026-07-18T14:23:50",
    )


class SetupMixin:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()


class TestSearch(SetupMixin):

    @patch(PATCH_SERVICE)
    def test_200과_페이지_응답을_camelCase로_반환한다(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.return_value = _page([_news()])
        mock_service_cls.return_value = mock_service

        response = self.client.get(BASE_URL, params={"q": "마이그레이션"})

        assert response.status_code == 200
        body = response.json()
        assert body["data"][0]["id"] == 900
        # api-v1과 같은 계약 — camelCase 직렬화
        assert body["data"][0]["sourceName"] == "GeekNews"
        assert body["data"][0]["categories"] == ["인프라"]
        assert body["data"][0]["publishedAt"].startswith("2026-07-18")
        assert body["totalCount"] == 1
        assert body["totalPages"] == 1
        assert body["first"] is True

    @patch(PATCH_SERVICE)
    def test_page_size_sort_기본값은_0_10_RELEVANCE다(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.return_value = _page()
        mock_service_cls.return_value = mock_service

        self.client.get(BASE_URL, params={"q": "마이그레이션"})

        kwargs = mock_service.search.call_args.kwargs
        assert kwargs["keyword"] == "마이그레이션"
        assert kwargs["page"] == 0
        assert kwargs["size"] == 10
        assert kwargs["sort"] is TechSearchSort.RELEVANCE

    @patch(PATCH_SERVICE)
    def test_전달한_페이징_정렬을_그대로_넘긴다(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.return_value = _page()
        mock_service_cls.return_value = mock_service

        self.client.get(BASE_URL, params={"q": "마이그레이션", "page": 2, "size": 20, "sort": "LATEST"})

        kwargs = mock_service.search.call_args.kwargs
        assert kwargs["page"] == 2
        assert kwargs["size"] == 20
        assert kwargs["sort"] is TechSearchSort.LATEST

    def test_q가_없으면_400이다(self):
        response = self.client.get(BASE_URL)

        assert response.status_code == 400

    def test_없는_정렬_값이면_400이다(self):
        response = self.client.get(BASE_URL, params={"q": "마이그레이션", "sort": "POPULAR"})

        assert response.status_code == 400

    @patch(PATCH_SERVICE)
    def test_인증_없이도_200이다(self, mock_service_cls):
        # 게시글 검색과 달리 개인화 값이 없어 토큰을 보지 않는다
        mock_service = MagicMock()
        mock_service.search.return_value = _page([_news()])
        mock_service_cls.return_value = mock_service

        response = self.client.get(BASE_URL, params={"q": "마이그레이션"})

        assert response.status_code == 200


class TestException(SetupMixin):

    @patch(PATCH_SERVICE)
    def test_검색어가_비면_400_BLANK_SEARCH_KEYWORD(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.side_effect = BlankSearchKeywordException()
        mock_service_cls.return_value = mock_service

        response = self.client.get(BASE_URL, params={"q": "  "})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "BLANK_SEARCH_KEYWORD"

    @patch(PATCH_SERVICE)
    def test_검색_설정이_없으면_503_SEARCH_UNAVAILABLE(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.side_effect = SearchUnavailableException()
        mock_service_cls.return_value = mock_service

        response = self.client.get(BASE_URL, params={"q": "마이그레이션"})

        assert response.status_code == 503
        assert response.json()["errorCode"] == "SEARCH_UNAVAILABLE"

    @patch(PATCH_SERVICE)
    def test_조회_한계를_넘으면_400_SEARCH_PAGE_TOO_DEEP(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.side_effect = SearchPageTooDeepException()
        mock_service_cls.return_value = mock_service

        response = self.client.get(BASE_URL, params={"q": "마이그레이션", "page": 5000})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "SEARCH_PAGE_TOO_DEEP"
