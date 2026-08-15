import os
from unittest.mock import MagicMock, patch

from fastapi.testclient import TestClient

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")
os.environ.setdefault("ADMIN_JWT_SECRET", "test-admin-jwt-secret-key-for-testing")

from app.config.database import get_db
from app.config.settings import get_settings
from app.core.exception import InvalidIndexRangeException
from app.domain.ums.admin_jwt_util import create_admin_token
from app.domain.ums.jwt_util import create_token
from app.main import app

# lru_cache 초기화
get_settings.cache_clear()

BASE_URL = "/api/v2/search/admin/tech/news/indexing"
PATCH_SERVICE = "app.domain.search.tech.news_indexing_router.AdminTechNewsIndexingService"


class SetupMixin:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)
        self.token = create_admin_token(1)
        self.headers = {"Authorization": f"Bearer {self.token}"}

    def teardown_method(self):
        app.dependency_overrides.clear()


class TestAdminTechPostIndexingRouter:

    class TestFullIndex(SetupMixin):

        @patch(PATCH_SERVICE)
        def test_202를_반환하고_백그라운드로_발행한다(self, mock_service_cls):
            mock_service = MagicMock()
            mock_service_cls.return_value = mock_service

            response = self.client.put(BASE_URL, headers=self.headers)

            assert response.status_code == 202
            # TestClient는 응답 후 백그라운드 작업까지 실행한다
            mock_service.full_index_in_background.assert_called_once()

        @patch(PATCH_SERVICE)
        def test_토큰이_없으면_401이고_발행하지_않는다(self, mock_service_cls):
            response = self.client.put(BASE_URL)

            assert response.status_code == 401
            mock_service_cls.assert_not_called()

        @patch(PATCH_SERVICE)
        def test_유저_토큰으로는_401이다(self, mock_service_cls):
            # 어드민 전용 — 유저 JWT는 secret·audience가 달라 검증을 통과할 수 없다
            user_token = create_token(1)
            response = self.client.put(BASE_URL, headers={"Authorization": f"Bearer {user_token}"})

            assert response.status_code == 401
            mock_service_cls.assert_not_called()

        @patch(PATCH_SERVICE)
        def test_유효하지_않은_토큰이면_401이다(self, mock_service_cls):
            response = self.client.put(BASE_URL, headers={"Authorization": "Bearer invalid-token"})

            assert response.status_code == 401
            mock_service_cls.assert_not_called()

    class TestIndexSingle(SetupMixin):

        @patch(PATCH_SERVICE)
        def test_202를_반환하고_해당_id로_발행한다(self, mock_service_cls):
            mock_service = MagicMock()
            mock_service_cls.return_value = mock_service

            response = self.client.put(f"{BASE_URL}/42", headers=self.headers)

            assert response.status_code == 202
            mock_service.index.assert_called_once_with(42)

        @patch(PATCH_SERVICE)
        def test_토큰이_없으면_401이다(self, mock_service_cls):
            response = self.client.put(f"{BASE_URL}/42")

            assert response.status_code == 401
            mock_service_cls.assert_not_called()

        @patch(PATCH_SERVICE)
        def test_id가_숫자가_아니면_400이다(self, mock_service_cls):
            # 경로 파라미터 타입 검증 — ValidationExceptionHandler가 422를 400으로 매핑한다(api-v1과 동일)
            response = self.client.put(f"{BASE_URL}/abc", headers=self.headers)

            assert response.status_code == 400

    class TestIndexRange(SetupMixin):

        @patch(PATCH_SERVICE)
        def test_202를_반환하고_범위로_발행한다(self, mock_service_cls):
            mock_service = MagicMock()
            mock_service_cls.return_value = mock_service

            response = self.client.put(f"{BASE_URL}/1/45", headers=self.headers)

            assert response.status_code == 202
            mock_service.index_range.assert_called_once_with(from_id=1, to_id=45)

        @patch(PATCH_SERVICE)
        def test_from이_to보다_크면_400이다(self, mock_service_cls):
            mock_service = MagicMock()
            mock_service.index_range.side_effect = InvalidIndexRangeException()
            mock_service_cls.return_value = mock_service

            response = self.client.put(f"{BASE_URL}/30/10", headers=self.headers)

            assert response.status_code == 400
            assert response.json()["errorCode"] == "INVALID_INDEX_RANGE"

        @patch(PATCH_SERVICE)
        def test_토큰이_없으면_401이다(self, mock_service_cls):
            response = self.client.put(f"{BASE_URL}/1/45")

            assert response.status_code == 401
            mock_service_cls.assert_not_called()
