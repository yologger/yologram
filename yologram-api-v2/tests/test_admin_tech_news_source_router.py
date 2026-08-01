import os
from datetime import datetime
from unittest.mock import MagicMock, patch

from fastapi.testclient import TestClient

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")
os.environ.setdefault("ADMIN_JWT_SECRET", "test-admin-jwt-secret-key-for-testing")

from app.config.database import get_db
from app.config.settings import get_settings
from app.domain.news.tech.model import TechNewsSource
from app.domain.ums.admin_jwt_util import create_admin_token
from app.domain.ums.jwt_util import create_token
from app.main import app

# lru_cache 초기화
get_settings.cache_clear()

BASE_URL = "/api/v2/news/admin/tech/sources"


def _source(source_id: int = 1, url: str = "https://news.hada.io/rss/news", is_active: bool = True) -> TechNewsSource:
    return TechNewsSource(
        id=source_id,
        name=f"소스 {source_id}",
        url=url,
        is_active=is_active,
        created_at=datetime(2026, 7, 18, 9, 0),
        modified_date=datetime(2026, 7, 18, 9, 0),
    )


class SetupMixin:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)
        self.token = create_admin_token(1)
        self.headers = {"Authorization": f"Bearer {self.token}"}

    def teardown_method(self):
        app.dependency_overrides.clear()


class TestAdminTechNewsSourceRouter:

    class TestGetSources(SetupMixin):

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_목록_조회_성공_200__camelCase_직렬화(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_all_order_by_id_asc.return_value = [
                _source(1),
                _source(2, url="https://techblog.woowahan.com/feed", is_active=False),
            ]
            mock_repo_cls.return_value = mock_repo

            response = self.client.get(BASE_URL, headers=self.headers)

            assert response.status_code == 200
            data = response.json()["data"]
            assert [s["id"] for s in data] == [1, 2]
            assert data[0]["name"] == "소스 1"
            assert data[0]["url"] == "https://news.hada.io/rss/news"
            assert data[0]["isActive"] is True
            assert data[0]["createdAt"] == "2026-07-18T09:00:00"
            assert data[0]["modifiedDate"] == "2026-07-18T09:00:00"
            assert data[1]["isActive"] is False

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_빈_목록_200(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_all_order_by_id_asc.return_value = []
            mock_repo_cls.return_value = mock_repo

            response = self.client.get(BASE_URL, headers=self.headers)

            assert response.status_code == 200
            assert response.json()["data"] == []

        def test_토큰_없음_401(self):
            response = self.client.get(BASE_URL)

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

        def test_유저_토큰_401(self):
            user_token = create_token(1)

            response = self.client.get(BASE_URL, headers={"Authorization": f"Bearer {user_token}"})

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    class TestCreate(SetupMixin):

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_생성_성공_201__isActive_생략_시_기본_true(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.exists_by_url.return_value = False
            mock_repo.save.return_value = _source(1)
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"name": "소스 1", "url": "https://news.hada.io/rss/news"},
            )

            assert response.status_code == 201
            data = response.json()["data"]
            assert data["id"] == 1
            assert data["isActive"] is True
            # 생략 시 기본 true로 저장 요청
            assert mock_repo.save.call_args[0][0].is_active is True

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_isActive_false로_생성_201(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.exists_by_url.return_value = False
            mock_repo.save.return_value = _source(1, is_active=False)
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"name": "소스 1", "url": "https://news.hada.io/rss/news", "isActive": False},
            )

            assert response.status_code == 201
            assert response.json()["data"]["isActive"] is False
            assert mock_repo.save.call_args[0][0].is_active is False

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_중복_url_409(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.exists_by_url.return_value = True
            mock_repo_cls.return_value = mock_repo

            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"name": "소스 1", "url": "https://news.hada.io/rss/news"},
            )

            assert response.status_code == 409
            body = response.json()
            assert body["errorCode"] == "NEWS_SOURCE_DUPLICATE"
            assert body["errorMessage"] == "이미 등록된 뉴스 소스 URL입니다."
            mock_repo.save.assert_not_called()

        def test_name_공백_400(self):
            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"name": "   ", "url": "https://news.hada.io/rss/news"},
            )

            assert response.status_code == 400
            body = response.json()
            assert body["errorCode"] == "VALIDATION_ERROR"
            assert body["errorMessage"] == "소스 이름을 입력해주세요"

        def test_name_101자_400(self):
            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"name": "a" * 101, "url": "https://news.hada.io/rss/news"},
            )

            assert response.status_code == 400
            body = response.json()
            assert body["errorCode"] == "VALIDATION_ERROR"
            assert body["errorMessage"] == "소스 이름은 1~100자여야 합니다"

        def test_name_누락_400(self):
            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"url": "https://news.hada.io/rss/news"},
            )

            assert response.status_code == 400
            assert response.json()["errorCode"] == "VALIDATION_ERROR"

        def test_url_공백_400(self):
            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"name": "소스 1", "url": "   "},
            )

            assert response.status_code == 400
            body = response.json()
            assert body["errorCode"] == "VALIDATION_ERROR"
            assert body["errorMessage"] == "RSS 피드 URL을 입력해주세요"

        def test_url_형식_오류_400(self):
            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"name": "소스 1", "url": "ftp://news.hada.io/rss/news"},
            )

            assert response.status_code == 400
            body = response.json()
            assert body["errorCode"] == "VALIDATION_ERROR"
            assert body["errorMessage"] == "URL은 http/https 형식이어야 합니다"

        def test_url_501자_400(self):
            long_url = "https://a.io/" + "a" * 488  # 총 501자

            response = self.client.post(
                BASE_URL,
                headers=self.headers,
                json={"name": "소스 1", "url": long_url},
            )

            assert response.status_code == 400
            body = response.json()
            assert body["errorCode"] == "VALIDATION_ERROR"
            assert body["errorMessage"] == "URL은 500자 이하여야 합니다"

        def test_토큰_없음_401(self):
            response = self.client.post(
                BASE_URL,
                json={"name": "소스 1", "url": "https://news.hada.io/rss/news"},
            )

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    class TestUpdate(SetupMixin):

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_name만_부분_갱신_200__나머지_필드_미변경(self, mock_repo_cls):
            source = _source(1)
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = source
            mock_repo.save.side_effect = lambda s: s
            mock_repo_cls.return_value = mock_repo

            response = self.client.patch(
                f"{BASE_URL}/1", headers=self.headers, json={"name": "새 이름"}
            )

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["name"] == "새 이름"
            assert data["url"] == "https://news.hada.io/rss/news"  # 미변경
            assert data["isActive"] is True  # 미변경
            mock_repo.exists_by_url_and_id_not.assert_not_called()  # url 미포함 시 중복 검사 없음

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_isActive만_부분_갱신_200(self, mock_repo_cls):
            source = _source(1)
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = source
            mock_repo.save.side_effect = lambda s: s
            mock_repo_cls.return_value = mock_repo

            response = self.client.patch(
                f"{BASE_URL}/1", headers=self.headers, json={"isActive": False}
            )

            assert response.status_code == 200
            data = response.json()["data"]
            assert data["isActive"] is False
            assert data["name"] == "소스 1"  # 미변경

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_없는_id_404(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.patch(
                f"{BASE_URL}/999", headers=self.headers, json={"name": "새 이름"}
            )

            assert response.status_code == 404
            body = response.json()
            assert body["errorCode"] == "NEWS_SOURCE_NOT_FOUND"
            assert body["errorMessage"] == "뉴스 소스를 찾을 수 없습니다."

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_다른_소스의_url로_변경_409(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = _source(1)
            mock_repo.exists_by_url_and_id_not.return_value = True
            mock_repo_cls.return_value = mock_repo

            response = self.client.patch(
                f"{BASE_URL}/1",
                headers=self.headers,
                json={"url": "https://techblog.woowahan.com/feed"},
            )

            assert response.status_code == 409
            assert response.json()["errorCode"] == "NEWS_SOURCE_DUPLICATE"
            mock_repo.save.assert_not_called()

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_자기_자신의_url로_변경은_허용_200(self, mock_repo_cls):
            source = _source(1)
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = source
            mock_repo.exists_by_url_and_id_not.return_value = False  # 자기 자신 제외 중복 없음
            mock_repo.save.side_effect = lambda s: s
            mock_repo_cls.return_value = mock_repo

            response = self.client.patch(
                f"{BASE_URL}/1",
                headers=self.headers,
                json={"url": "https://news.hada.io/rss/news"},
            )

            assert response.status_code == 200
            assert response.json()["data"]["url"] == "https://news.hada.io/rss/news"
            mock_repo.exists_by_url_and_id_not.assert_called_once_with(
                "https://news.hada.io/rss/news", 1
            )

        def test_url_형식_오류_400(self):
            response = self.client.patch(
                f"{BASE_URL}/1", headers=self.headers, json={"url": "not-a-url"}
            )

            assert response.status_code == 400
            body = response.json()
            assert body["errorCode"] == "VALIDATION_ERROR"
            assert body["errorMessage"] == "URL은 http/https 형식이어야 합니다"

        def test_name_공백_400(self):
            response = self.client.patch(
                f"{BASE_URL}/1", headers=self.headers, json={"name": "  "}
            )

            assert response.status_code == 400
            body = response.json()
            assert body["errorCode"] == "VALIDATION_ERROR"
            assert body["errorMessage"] == "소스 이름을 입력해주세요"

        def test_토큰_없음_401(self):
            response = self.client.patch(f"{BASE_URL}/1", json={"name": "새 이름"})

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    class TestDelete(SetupMixin):

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_삭제_성공_204(self, mock_repo_cls):
            source = _source(1)
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = source
            mock_repo_cls.return_value = mock_repo

            response = self.client.delete(f"{BASE_URL}/1", headers=self.headers)

            assert response.status_code == 204
            assert response.content == b""
            mock_repo.delete.assert_called_once_with(source)

        @patch("app.domain.news.tech.admin_service.TechNewsSourceRepository")
        def test_없는_id_404(self, mock_repo_cls):
            mock_repo = MagicMock()
            mock_repo.find_by_id.return_value = None
            mock_repo_cls.return_value = mock_repo

            response = self.client.delete(f"{BASE_URL}/999", headers=self.headers)

            assert response.status_code == 404
            assert response.json()["errorCode"] == "NEWS_SOURCE_NOT_FOUND"
            mock_repo.delete.assert_not_called()

        def test_토큰_없음_401(self):
            response = self.client.delete(f"{BASE_URL}/1")

            assert response.status_code == 401
            assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"
