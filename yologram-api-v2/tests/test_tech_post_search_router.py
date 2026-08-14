import os
from unittest.mock import MagicMock, patch

from fastapi.testclient import TestClient

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")
os.environ.setdefault("ADMIN_JWT_SECRET", "test-admin-jwt-secret-key-for-testing")

from app.config.database import get_db
from app.config.settings import get_settings
from app.core.exception import BlankSearchKeywordException, SearchPageTooDeepException
from app.core.response import ApiEnvelopPage
from app.domain.pms.tech.schema import PostAuthor, PostMetrics, PostSummaryResponse
from app.domain.search.tech.model import TechPostSearchSort
from app.domain.ums.jwt_util import create_token
from app.main import app

get_settings.cache_clear()

BASE_URL = "/api/v2/search/tech/posts"
PATCH_SERVICE = "app.domain.search.tech.search_router.TechPostSearchService"


def _page(items: list[PostSummaryResponse] | None = None) -> ApiEnvelopPage:
    data = items or []
    return ApiEnvelopPage(
        data=data, page=0, size=10, total_pages=1, total_count=len(data), first=True, last=True
    )


def _summary(post_id: int = 1200) -> PostSummaryResponse:
    return PostSummaryResponse(
        id=post_id,
        author=PostAuthor(uid=12, nickname="tester0"),
        title="제목",
        content="본문",
        category_ids=[1],
        metrics=PostMetrics(comment_count=2, like_count=3, view_count=4, liked_by_me=False),
        created_at="2026-07-18T14:23:50",
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
        mock_service.search.return_value = _page([_summary()])
        mock_service_cls.return_value = mock_service

        response = self.client.get(BASE_URL, params={"q": "제미나이"})

        assert response.status_code == 200
        body = response.json()
        assert body["data"][0]["id"] == 1200
        assert body["data"][0]["author"]["nickname"] == "tester0"
        # api-v1과 같은 계약 — camelCase 직렬화
        assert body["data"][0]["metrics"]["viewCount"] == 4
        assert body["data"][0]["categoryIds"] == [1]
        assert body["totalCount"] == 1
        assert body["totalPages"] == 1
        assert body["first"] is True

    @patch(PATCH_SERVICE)
    def test_page_size_sort_기본값은_0_10_RELEVANCE다(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.return_value = _page()
        mock_service_cls.return_value = mock_service

        self.client.get(BASE_URL, params={"q": "제미나이"})

        kwargs = mock_service.search.call_args.kwargs
        assert kwargs["keyword"] == "제미나이"
        assert kwargs["page"] == 0
        assert kwargs["size"] == 10
        assert kwargs["sort"] is TechPostSearchSort.RELEVANCE
        assert kwargs["viewer_uid"] is None

    @patch(PATCH_SERVICE)
    def test_전달한_페이징_정렬을_그대로_넘긴다(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.return_value = _page()
        mock_service_cls.return_value = mock_service

        self.client.get(BASE_URL, params={"q": "제미나이", "page": 2, "size": 20, "sort": "LATEST"})

        kwargs = mock_service.search.call_args.kwargs
        assert kwargs["page"] == 2
        assert kwargs["size"] == 20
        assert kwargs["sort"] is TechPostSearchSort.LATEST

    def test_q가_없으면_400이다(self):
        response = self.client.get(BASE_URL)

        assert response.status_code == 400

    def test_없는_정렬_값이면_400이다(self):
        response = self.client.get(BASE_URL, params={"q": "제미나이", "sort": "POPULAR"})

        assert response.status_code == 400


class TestOptionalAuth(SetupMixin):

    @patch(PATCH_SERVICE)
    def test_토큰이_있으면_viewer_uid를_넘긴다(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.return_value = _page()
        mock_service_cls.return_value = mock_service
        token = create_token(12)

        response = self.client.get(
            BASE_URL, params={"q": "제미나이"}, headers={"Authorization": f"Bearer {token}"}
        )

        assert response.status_code == 200
        assert mock_service.search.call_args.kwargs["viewer_uid"] == 12

    @patch(PATCH_SERVICE)
    def test_토큰이_없으면_비로그인으로_처리한다(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.return_value = _page()
        mock_service_cls.return_value = mock_service

        self.client.get(BASE_URL, params={"q": "제미나이"})

        assert mock_service.search.call_args.kwargs["viewer_uid"] is None

    def test_토큰이_유효하지_않으면_401이다(self):
        # 선택 인증이지만 헤더가 있으면 검증한다 — 무효 토큰을 비로그인으로 흘리지 않는다
        response = self.client.get(
            BASE_URL, params={"q": "제미나이"}, headers={"Authorization": "Bearer invalid-token"}
        )

        assert response.status_code == 401


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
    def test_조회_한계를_넘으면_400_SEARCH_PAGE_TOO_DEEP(self, mock_service_cls):
        mock_service = MagicMock()
        mock_service.search.side_effect = SearchPageTooDeepException()
        mock_service_cls.return_value = mock_service

        response = self.client.get(BASE_URL, params={"q": "제미나이", "page": 5000})

        assert response.status_code == 400
        assert response.json()["errorCode"] == "SEARCH_PAGE_TOO_DEEP"
