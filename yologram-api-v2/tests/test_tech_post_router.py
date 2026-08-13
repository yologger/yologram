import json
import os
from datetime import datetime
from unittest.mock import MagicMock, patch

os.environ.setdefault("JWT_SECRET", "test-jwt-secret-key-for-testing")

from botocore.exceptions import ClientError
from fastapi.testclient import TestClient

from app.config.database import get_db
from app.domain.pms.tech.model import TechPost, TechPostCategoryMapping, TechPostWithCounts
from app.domain.ums.auth_dependency import get_authenticated_user, get_optional_authenticated_user
from app.domain.ums.auth_schema import AuthData
from app.main import app


def _saved_post(post_id: int = 10) -> TechPost:
    post = TechPost(user_id=1, content="내용")
    post.id = post_id
    return post


class TestTechPostRouter:

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    def _authenticate(self):
        app.dependency_overrides[get_authenticated_user] = lambda: AuthData(uid=1, access_token="t")

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_정상_작성_시_201(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        self._authenticate()
        mock_post_repo = MagicMock()
        mock_post_repo.save.return_value = _saved_post(10)
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_client = MagicMock()
        mock_client.all_active.return_value = True
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

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_카테고리가_테크_게시판_것이_아니면_400(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        self._authenticate()
        mock_post_repo_cls.return_value = MagicMock()
        mock_pc_repo_cls.return_value = MagicMock()
        mock_client = MagicMock()
        mock_client.all_active.return_value = False
        mock_client_cls.return_value = mock_client

        response = self.client.post(
            "/api/v2/pms/tech/posts",
            json={"content": "내용", "categoryIds": [99]},
        )

        assert response.status_code == 400
        assert response.json()["errorCode"] == "INVALID_POST_CATEGORY"

    def test_다른_section_경로면_404(self):
        # 테이블 분리로 경로가 tech 고정 — 다른 섹션 경로는 라우트가 없어 404
        self._authenticate()

        response = self.client.post("/api/v2/pms/unknown/posts", json={"content": "내용", "categoryIds": [1]})

        assert response.status_code == 404
        assert response.json()["errorCode"] == "NOT_FOUND"

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

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_상세_조회_시_200(self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls):
        post = _saved_post(1)
        post.user_id = 12
        post.title = "제목"
        post.created_at = datetime(2026, 1, 1, 0, 0)
        mock_post_repo = MagicMock()
        # 상세는 프로젝션(find_post_with_counts)으로 조회 — 카운트는 coalesce 실값
        mock_post_repo.find_post_with_counts.return_value = TechPostWithCounts(
            post=post, comment_count=2, like_count=5
        )
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_id.return_value = [TechPostCategoryMapping(post_id=1, category_id=1)]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nickname.return_value = "tester"
        mock_user_cls.return_value = mock_user

        response = self.client.get("/api/v2/pms/tech/posts/1")

        assert response.status_code == 200
        body = response.json()["data"]
        assert body["id"] == 1
        assert body["section"] == "TECH"
        assert body["author"]["nickname"] == "tester"
        assert body["content"] == "내용"
        assert body["categoryIds"] == [1]
        # 카운트는 metrics 객체로 중첩 (평면 필드 제거), 비로그인이라 likedByMe False
        assert body["metrics"] == {"commentCount": 2, "likeCount": 5, "likedByMe": False}
        assert "commentCount" not in body
        assert "likeCount" not in body

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_존재하지_않는_게시글이면_404(self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_post_with_counts.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo

        response = self.client.get("/api/v2/pms/tech/posts/99")

        assert response.status_code == 404
        assert response.json()["errorCode"] == "POST_NOT_FOUND"

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_목록_조회_시_200과_data_nextCursor(self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls):
        post = _saved_post(2)
        post.user_id = 12
        post.title = "제목"
        post.created_at = datetime(2026, 1, 1, 0, 0)
        mock_post_repo = MagicMock()
        # 목록도 프로젝션(TechPostWithCounts) 반환 — 카운트는 coalesce 실값
        mock_post_repo.find_posts.return_value = [TechPostWithCounts(post=post, comment_count=3, like_count=1)]
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = [TechPostCategoryMapping(post_id=2, category_id=1)]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {12: "tester"}
        mock_user_cls.return_value = mock_user

        response = self.client.get("/api/v2/pms/tech/posts?size=5")

        assert response.status_code == 200
        body = response.json()
        assert body["data"][0]["id"] == 2
        assert body["data"][0]["section"] == "TECH"
        assert body["data"][0]["author"]["nickname"] == "tester"
        assert body["data"][0]["categoryIds"] == [1]
        assert body["data"][0]["metrics"] == {"commentCount": 3, "likeCount": 1, "likedByMe": False}
        assert body["nextCursor"] is not None

    def test_목록_조회_시_다른_section_경로면_404(self):
        response = self.client.get("/api/v2/pms/unknown/posts")

        assert response.status_code == 404
        assert response.json()["errorCode"] == "NOT_FOUND"

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_내_글_목록_조회_시_200과_data_nextCursor(self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, mock_like_repo_cls):
        self._authenticate()
        post = _saved_post(5)
        post.user_id = 1
        post.title = "제목"
        post.created_at = datetime(2026, 1, 1, 0, 0)
        mock_post_repo = MagicMock()
        # 내 글 목록도 프로젝션(TechPostWithCounts) 반환 — count row 없는 글은 coalesce 0
        mock_post_repo.find_my_posts_by_cursor.return_value = [TechPostWithCounts(post=post, comment_count=0, like_count=0)]
        mock_post_repo_cls.return_value = mock_post_repo
        mock_like_repo = MagicMock()
        mock_like_repo.find_liked_post_ids.return_value = set()
        mock_like_repo_cls.return_value = mock_like_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {1: "me"}
        mock_user_cls.return_value = mock_user

        response = self.client.get("/api/v2/pms/posts/me?size=5")

        assert response.status_code == 200
        body = response.json()
        assert body["data"][0]["id"] == 5
        assert body["data"][0]["author"]["nickname"] == "me"
        assert body["nextCursor"] is not None

    def test_내_글_목록_조회_시_미인증_401(self):
        response = self.client.get("/api/v2/pms/posts/me")

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    def test_내_글_목록_조회_시_유효하지_않은_section이면_400(self):
        self._authenticate()

        response = self.client.get("/api/v2/pms/posts/me?section=unknown")

        assert response.status_code == 400
        assert response.json()["errorCode"] == "INVALID_SECTION"

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_게시글_수정_시_204(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        self._authenticate()
        post = _saved_post(1)
        post.user_id = 1
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat = MagicMock()
        mock_cat.all_active.return_value = True
        mock_cat_cls.return_value = mock_cat

        response = self.client.patch(
            "/api/v2/pms/tech/posts/1",
            json={"title": "수정", "content": "수정 내용", "categoryIds": [1]},
        )

        assert response.status_code == 204

    def test_게시글_수정_미인증_401(self):
        response = self.client.patch(
            "/api/v2/pms/tech/posts/1",
            json={"content": "수정 내용", "categoryIds": [1]},
        )

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_본인_글이_아니면_403(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        self._authenticate()
        post = _saved_post(1)
        post.user_id = 99
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat_cls.return_value = MagicMock()

        response = self.client.patch(
            "/api/v2/pms/tech/posts/1",
            json={"content": "수정 내용", "categoryIds": [1]},
        )

        assert response.status_code == 403
        assert response.json()["errorCode"] == "POST_FORBIDDEN"

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_존재하지_않는_글_수정_시_404(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        self._authenticate()
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat_cls.return_value = MagicMock()

        response = self.client.patch(
            "/api/v2/pms/tech/posts/99",
            json={"content": "수정 내용", "categoryIds": [1]},
        )

        assert response.status_code == 404
        assert response.json()["errorCode"] == "POST_NOT_FOUND"

    def test_수정_시_내용_누락이면_400(self):
        self._authenticate()
        response = self.client.patch(
            "/api/v2/pms/tech/posts/1",
            json={"categoryIds": [1]},
        )

        assert response.status_code == 400
        assert response.json()["errorCode"] == "VALIDATION_ERROR"

    @patch("app.domain.pms.tech.service.LocalCommentApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_게시글_삭제_시_204(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_cleanup_cls):
        self._authenticate()
        post = _saved_post(1)
        post.user_id = 1
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat_cls.return_value = MagicMock()
        mock_cleanup_cls.return_value = MagicMock()

        response = self.client.delete("/api/v2/pms/tech/posts/1")

        assert response.status_code == 204

    def test_게시글_삭제_미인증_401(self):
        response = self.client.delete("/api/v2/pms/tech/posts/1")

        assert response.status_code == 401
        assert response.json()["errorCode"] == "AUTH_INVALID_TOKEN"

    @patch("app.domain.pms.tech.service.LocalCommentApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_본인_글이_아니면_삭제_403(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_cleanup_cls):
        self._authenticate()
        post = _saved_post(1)
        post.user_id = 99
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat_cls.return_value = MagicMock()
        mock_cleanup_cls.return_value = MagicMock()

        response = self.client.delete("/api/v2/pms/tech/posts/1")

        assert response.status_code == 403
        assert response.json()["errorCode"] == "POST_FORBIDDEN"

    @patch("app.domain.pms.tech.service.LocalCommentApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_존재하지_않는_글_삭제_시_404(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_cleanup_cls):
        self._authenticate()
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat_cls.return_value = MagicMock()
        mock_cleanup_cls.return_value = MagicMock()

        response = self.client.delete("/api/v2/pms/tech/posts/99")

        assert response.status_code == 404
        assert response.json()["errorCode"] == "POST_NOT_FOUND"


class TestTechPostRouterPostViewEvent:
    """상세 조회 엔드포인트의 조회 이벤트 발행 — IP 추출·발행 여부·실패 격리를 라우터 경유로 검증."""

    def setup_method(self):
        self.mock_db = MagicMock()
        app.dependency_overrides[get_db] = lambda: self.mock_db
        # TestClient 기본 접속 주소는 "testclient" — X-Forwarded-For 폴백 검증에 사용
        self.client = TestClient(app)

    def teardown_method(self):
        app.dependency_overrides.clear()

    @staticmethod
    def _stub_repositories(mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, found: bool = True):
        post = _saved_post(1)
        post.user_id = 12
        post.created_at = datetime(2026, 1, 1, 0, 0)
        mock_post_repo = MagicMock()
        mock_post_repo.find_post_with_counts.return_value = (
            TechPostWithCounts(post=post, comment_count=0, like_count=0) if found else None
        )
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock(find_by_post_id=MagicMock(return_value=[]))
        mock_user_cls.return_value = MagicMock(find_nickname=MagicMock(return_value="tester"))

    @patch("app.domain.pms.tech.service.KinesisPostViewEventPublisher")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_X_Forwarded_For_첫_값이_이벤트_ip가_된다(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, mock_publisher_cls
    ):
        self._stub_repositories(mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls)

        response = self.client.get(
            "/api/v2/pms/tech/posts/1",
            headers={"X-Forwarded-For": "1.2.3.4, 70.41.3.18"},
        )

        assert response.status_code == 200
        event = mock_publisher_cls.return_value.publish.call_args.args[0]
        assert event.ip == "1.2.3.4"
        assert event.uid is None  # 비로그인

    @patch("app.domain.pms.tech.service.KinesisPostViewEventPublisher")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_헤더가_없으면_접속_주소로_폴백한다(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, mock_publisher_cls
    ):
        self._stub_repositories(mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls)

        response = self.client.get("/api/v2/pms/tech/posts/1")

        assert response.status_code == 200
        event = mock_publisher_cls.return_value.publish.call_args.args[0]
        assert event.ip == "testclient"  # TestClient의 접속 주소

    @patch("app.domain.pms.tech.service.KinesisPostViewEventPublisher")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_로그인_조회면_uid가_담긴다(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, mock_publisher_cls
    ):
        self._stub_repositories(mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls)
        # 선택 인증 의존성 재사용 — 헤더가 있으면 검증된 uid가 이벤트에 담긴다
        app.dependency_overrides[get_optional_authenticated_user] = lambda: AuthData(uid=7, access_token="t")

        response = self.client.get("/api/v2/pms/tech/posts/1", headers={"X-Forwarded-For": "1.2.3.4"})

        assert response.status_code == 200
        event = mock_publisher_cls.return_value.publish.call_args.args[0]
        assert event.uid == 7

    @patch("app.domain.pms.tech.service.KinesisPostViewEventPublisher")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_404면_발행하지_않는다(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, mock_publisher_cls
    ):
        self._stub_repositories(mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, found=False)

        response = self.client.get("/api/v2/pms/tech/posts/99")

        assert response.status_code == 404
        mock_publisher_cls.return_value.publish.assert_not_called()

    @patch("app.infra.event.post_view_event_publisher.get_kinesis_client")
    @patch("app.infra.event.post_view_event_publisher.get_settings")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_스트림이_설정되면_put_record로_발행한다(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, mock_get_settings, mock_get_client
    ):
        # 실제 publisher 경로(설정 → 클라이언트 → put_record)까지 태워 페이로드를 확인
        self._stub_repositories(mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls)
        mock_get_settings.return_value = MagicMock(post_view_stream_name="yologram-post-view-event-test")
        mock_client = MagicMock()
        mock_get_client.return_value = mock_client

        response = self.client.get("/api/v2/pms/tech/posts/1", headers={"X-Forwarded-For": "1.2.3.4"})

        assert response.status_code == 200
        kwargs = mock_client.put_record.call_args.kwargs
        assert kwargs["StreamName"] == "yologram-post-view-event-test"
        assert kwargs["PartitionKey"] == "1"
        payload = json.loads(kwargs["Data"].decode("utf-8"))
        assert payload["eventType"] == "POST_VIEW"
        assert payload["section"] == "TECH"
        assert payload["postId"] == 1
        assert payload["uid"] is None
        assert payload["ip"] == "1.2.3.4"

    @patch("app.infra.event.post_view_event_publisher.get_kinesis_client")
    @patch("app.infra.event.post_view_event_publisher.get_settings")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_발행이_실패해도_상세_조회는_200(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, mock_get_settings, mock_get_client
    ):
        self._stub_repositories(mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls)
        mock_get_settings.return_value = MagicMock(post_view_stream_name="yologram-post-view-event-test")
        mock_client = MagicMock()
        mock_client.put_record.side_effect = ClientError(
            {"Error": {"Code": "ResourceNotFoundException", "Message": "no stream"}}, "PutRecord"
        )
        mock_get_client.return_value = mock_client

        response = self.client.get("/api/v2/pms/tech/posts/1")

        assert response.status_code == 200
        assert response.json()["data"]["id"] == 1

    @patch("app.infra.event.post_view_event_publisher.get_kinesis_client")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_스트림_미설정_로컬_기본이면_발행_스킵(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls, mock_get_client
    ):
        # 설정 기본값(빈 문자열)을 그대로 사용 — 로컬·테스트에서 prod 스트림이 오염되지 않는지 확인
        self._stub_repositories(mock_post_repo_cls, mock_pc_repo_cls, mock_user_cls)

        response = self.client.get("/api/v2/pms/tech/posts/1")

        assert response.status_code == 200
        mock_get_client.assert_not_called()
