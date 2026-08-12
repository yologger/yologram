from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import (
    InvalidPostCategoryException,
    InvalidSectionException,
    PostForbiddenException,
    PostNotFoundException,
)
from app.domain.pms.tech.cursor import TechPostCursor
from app.domain.pms.tech.model import TechPost, TechPostCategoryMapping, TechPostWithCounts
from app.domain.pms.tech.schema import CreatePostRequest, UpdatePostRequest
from app.domain.pms.tech.service import TechPostService


def _saved_post(post_id: int = 10) -> TechPost:
    post = TechPost(user_id=1, content="내용")
    post.id = post_id
    return post


def _post(post_id: int, user_id: int | None = None) -> TechPost:
    post = TechPost(user_id=user_id or post_id, content=f"내용{post_id}")
    post.id = post_id
    post.created_at = datetime(2026, 1, 1, 0, 0)
    return post


def _post_with_counts(
    post_id: int, comment_count: int = 0, like_count: int = 0, user_id: int | None = None
) -> TechPostWithCounts:
    """리포지토리 프로젝션(outerjoin+coalesce 결과) 모형 — 목록·상세 조회 반환값."""
    return TechPostWithCounts(
        post=_post(post_id, user_id=user_id), comment_count=comment_count, like_count=like_count
    )


class TestTechPostService:

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_정상_작성_시_게시글과_카테고리를_저장하고_id를_반환(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.save.return_value = _saved_post(10)
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_client = MagicMock()
        mock_client.all_active.return_value = True
        mock_client_cls.return_value = mock_client

        service = TechPostService(MagicMock())
        result = service.create(1, CreatePostRequest(content="내용", category_ids=[1, 2]))

        assert result.id == 10
        assert mock_pc_repo.save.call_count == 2

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_카테고리가_테크_게시판_것이_아니면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo
        mock_client = MagicMock()
        mock_client.all_active.return_value = False
        mock_client_cls.return_value = mock_client

        service = TechPostService(MagicMock())

        with pytest.raises(InvalidPostCategoryException):
            service.create(1, CreatePostRequest(content="내용", category_ids=[99]))

        mock_post_repo.save.assert_not_called()


class TestTechPostServiceUpdate:

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_본인_글이면_수정_후_카테고리_교체(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        post = _post(1, user_id=1)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat = MagicMock()
        mock_cat.all_active.return_value = True
        mock_cat_cls.return_value = mock_cat

        service = TechPostService(MagicMock())
        service.update(1, 1, UpdatePostRequest(title="새 제목", content="새 내용", category_ids=[2, 3]))

        assert post.title == "새 제목"
        assert post.content == "새 내용"
        mock_pc_repo.delete_by_post_id.assert_called_once_with(1)
        assert mock_pc_repo.save.call_count == 2

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_존재하지_않는_글이면_404(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat_cls.return_value = MagicMock()

        service = TechPostService(MagicMock())
        with pytest.raises(PostNotFoundException):
            service.update(99, 1, UpdatePostRequest(content="내용", category_ids=[1]))

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_본인_글이_아니면_403(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        post = _post(1, user_id=99)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat_cls.return_value = MagicMock()

        service = TechPostService(MagicMock())
        with pytest.raises(PostForbiddenException):
            service.update(1, 1, UpdatePostRequest(content="내용", category_ids=[1]))

        mock_pc_repo.delete_by_post_id.assert_not_called()

    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_카테고리가_테크_게시판_것이_아니면_400(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        post = _post(1, user_id=1)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat = MagicMock()
        mock_cat.all_active.return_value = False
        mock_cat_cls.return_value = mock_cat

        service = TechPostService(MagicMock())
        with pytest.raises(InvalidPostCategoryException):
            service.update(1, 1, UpdatePostRequest(content="내용", category_ids=[99]))

        mock_pc_repo.delete_by_post_id.assert_not_called()


class TestTechPostServiceDelete:

    @patch("app.domain.pms.tech.service.LocalCommentApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_본인_글이면_카테고리_매핑과_댓글_제거_후_게시글_삭제(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_cleanup_cls
    ):
        post = _post(1, user_id=1)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat_cls.return_value = MagicMock()
        mock_cleanup = MagicMock()
        mock_cleanup_cls.return_value = mock_cleanup

        service = TechPostService(MagicMock())
        service.delete(1, 1)

        mock_pc_repo.delete_by_post_id.assert_called_once_with(1)
        mock_cleanup.delete_by_post_id.assert_called_once_with(1)
        mock_post_repo.delete.assert_called_once_with(post)

    @patch("app.domain.pms.tech.service.LocalCommentApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_존재하지_않는_글이면_404(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_cleanup_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat_cls.return_value = MagicMock()
        mock_cleanup = MagicMock()
        mock_cleanup_cls.return_value = mock_cleanup

        service = TechPostService(MagicMock())
        with pytest.raises(PostNotFoundException):
            service.delete(99, 1)

        mock_pc_repo.delete_by_post_id.assert_not_called()
        mock_cleanup.delete_by_post_id.assert_not_called()
        mock_post_repo.delete.assert_not_called()

    @patch("app.domain.pms.tech.service.LocalCommentApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_본인_글이_아니면_403(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_cleanup_cls):
        post = _post(1, user_id=99)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat_cls.return_value = MagicMock()
        mock_cleanup = MagicMock()
        mock_cleanup_cls.return_value = mock_cleanup

        service = TechPostService(MagicMock())
        with pytest.raises(PostForbiddenException):
            service.delete(1, 1)

        mock_pc_repo.delete_by_post_id.assert_not_called()
        mock_cleanup.delete_by_post_id.assert_not_called()
        mock_post_repo.delete.assert_not_called()


class TestTechPostServiceGetPost:

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_게시글과_카테고리_작성자_닉네임_metrics를_반환(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        post = TechPost(user_id=12, title="제목", content="내용")
        post.id = 1
        post.created_at = datetime(2026, 1, 1, 0, 0)
        mock_post_repo = MagicMock()
        # 상세는 프로젝션(find_post_with_counts)으로 조회 — 카운트는 coalesce 실값
        mock_post_repo.find_post_with_counts.return_value = TechPostWithCounts(
            post=post, comment_count=2, like_count=5
        )
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_id.return_value = [
            TechPostCategoryMapping(post_id=1, category_id=1),
            TechPostCategoryMapping(post_id=1, category_id=2),
        ]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nickname.return_value = "tester"
        mock_user_cls.return_value = mock_user
        mock_like_repo = MagicMock()
        mock_like_repo_cls.return_value = mock_like_repo

        service = TechPostService(MagicMock())
        result = service.get_post(1)

        assert result.id == 1
        assert result.section == "TECH"
        assert result.author.uid == 12
        assert result.author.nickname == "tester"
        assert result.category_ids == [1, 2]
        assert result.content == "내용"
        assert result.metrics.comment_count == 2
        assert result.metrics.like_count == 5
        # 비로그인(viewer_uid 없음) — likedByMe False, 원장 조회도 하지 않는다
        assert result.metrics.liked_by_me is False
        mock_like_repo.exists_by_post_id_and_uid.assert_not_called()

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_로그인_유저가_좋아요한_글이면_likedByMe_true(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        mock_post_repo = MagicMock()
        mock_post_repo.find_post_with_counts.return_value = TechPostWithCounts(
            post=_post(1, user_id=12), comment_count=0, like_count=1
        )
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_id.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user_cls.return_value = MagicMock(find_nickname=MagicMock(return_value="tester"))
        mock_like_repo = MagicMock()
        mock_like_repo.exists_by_post_id_and_uid.return_value = True
        mock_like_repo_cls.return_value = mock_like_repo

        service = TechPostService(MagicMock())
        result = service.get_post(1, viewer_uid=7)

        assert result.metrics.liked_by_me is True
        mock_like_repo.exists_by_post_id_and_uid.assert_called_once_with(1, 7)

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_로그인_유저가_좋아요하지_않은_글이면_likedByMe_false(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        mock_post_repo = MagicMock()
        mock_post_repo.find_post_with_counts.return_value = TechPostWithCounts(
            post=_post(1, user_id=12), comment_count=0, like_count=1
        )
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_id.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user_cls.return_value = MagicMock(find_nickname=MagicMock(return_value="tester"))
        mock_like_repo = MagicMock()
        mock_like_repo.exists_by_post_id_and_uid.return_value = False
        mock_like_repo_cls.return_value = mock_like_repo

        service = TechPostService(MagicMock())
        result = service.get_post(1, viewer_uid=7)

        assert result.metrics.liked_by_me is False

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_존재하지_않는_게시글이면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_post_with_counts.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo

        service = TechPostService(MagicMock())

        with pytest.raises(PostNotFoundException):
            service.get_post(99)


class TestTechPostServiceGetPosts:

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_결과가_있으면_마지막_글_id를_nextCursor로_반환(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts.return_value = [
            _post_with_counts(3, comment_count=5, like_count=2),
            _post_with_counts(2),
        ]
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = [TechPostCategoryMapping(post_id=3, category_id=10)]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {3: "u3", 2: "u2"}
        mock_user_cls.return_value = mock_user
        mock_like_repo = MagicMock()
        mock_like_repo_cls.return_value = mock_like_repo

        service = TechPostService(MagicMock())
        result = service.get_posts_by_cursor(None, None, 2)

        assert [p.id for p in result.data] == [3, 2]
        assert result.data[0].section == "TECH"
        assert result.data[0].category_ids == [10]
        assert result.data[0].author.nickname == "u3"
        # 카운트는 프로젝션 실값 그대로 (count row 없는 글은 coalesce 0)
        assert [p.metrics.comment_count for p in result.data] == [5, 0]
        assert [p.metrics.like_count for p in result.data] == [2, 0]
        # 비로그인 — 전부 False, 원장 배치 조회도 하지 않는다
        assert [p.metrics.liked_by_me for p in result.data] == [False, False]
        mock_like_repo.find_liked_post_ids.assert_not_called()
        assert result.next_cursor == TechPostCursor.encode(2)

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_로그인_시_좋아요한_글만_likedByMe_true(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts.return_value = [_post_with_counts(3, like_count=2), _post_with_counts(2)]
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))
        mock_like_repo = MagicMock()
        # viewer(7)는 글 3만 좋아요 — 원장 IN 조회 1번으로 배치 판정
        mock_like_repo.find_liked_post_ids.return_value = {3}
        mock_like_repo_cls.return_value = mock_like_repo

        service = TechPostService(MagicMock())
        result = service.get_posts_by_cursor(None, None, 2, viewer_uid=7)

        assert [p.metrics.liked_by_me for p in result.data] == [True, False]
        mock_like_repo.find_liked_post_ids.assert_called_once_with(7, [3, 2])

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_결과가_없으면_빈_목록과_None_nextCursor(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user

        service = TechPostService(MagicMock())
        result = service.get_posts_by_cursor(None, None, 20)

        assert result.data == []
        assert result.next_cursor is None

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_cursor가_주어지면_디코딩한_id로_조회(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user

        service = TechPostService(MagicMock())
        service.get_posts_by_cursor(None, TechPostCursor.encode(5), 20)

        mock_post_repo.find_posts.assert_called_once_with(None, 5, 20)

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_size가_최대치를_넘으면_50으로_제한(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user

        service = TechPostService(MagicMock())
        service.get_posts_by_cursor(None, None, 100)

        mock_post_repo.find_posts.assert_called_once_with(None, None, 50)


class TestTechPostServiceGetMyPosts:

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_내_글_cursor_목록과_nextCursor를_반환(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        mock_post_repo = MagicMock()
        mock_post_repo.find_my_posts_by_cursor.return_value = [
            _post_with_counts(3, comment_count=1, like_count=4, user_id=1),
            _post_with_counts(2, user_id=1),
        ]
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = [TechPostCategoryMapping(post_id=3, category_id=10)]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {1: "me"}
        mock_user_cls.return_value = mock_user
        mock_like_repo = MagicMock()
        # 본인이 자기 글 3에 좋아요한 상태 — 내 글 목록도 likedByMe 배치 판정 (viewer = 본인)
        mock_like_repo.find_liked_post_ids.return_value = {3}
        mock_like_repo_cls.return_value = mock_like_repo

        service = TechPostService(MagicMock())
        result = service.get_my_posts_by_cursor(1, None, None, 2)

        assert [p.id for p in result.data] == [3, 2]
        assert result.data[0].category_ids == [10]
        assert [p.metrics.comment_count for p in result.data] == [1, 0]
        assert [p.metrics.like_count for p in result.data] == [4, 0]
        assert [p.metrics.liked_by_me for p in result.data] == [True, False]
        mock_like_repo.find_liked_post_ids.assert_called_once_with(1, [3, 2])
        assert result.next_cursor == TechPostCursor.encode(2)

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_내_글_section_tech_지정시_정상_조회(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        mock_post_repo = MagicMock()
        mock_post_repo.find_my_posts_by_cursor.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user
        mock_like_repo_cls.return_value = MagicMock()

        service = TechPostService(MagicMock())
        service.get_my_posts_by_cursor(1, "tech", None, 20)

        mock_post_repo.find_my_posts_by_cursor.assert_called_once_with(1, None, 20)

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_내_글_section_대문자_TECH도_허용(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        mock_post_repo = MagicMock()
        mock_post_repo.find_my_posts_by_cursor.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user
        mock_like_repo_cls.return_value = MagicMock()

        service = TechPostService(MagicMock())
        service.get_my_posts_by_cursor(1, "TECH", None, 20)

        mock_post_repo.find_my_posts_by_cursor.assert_called_once_with(1, None, 20)

    @patch("app.domain.pms.tech.service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_내_글_section_없으면_전체_조회(
        self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls, mock_like_repo_cls
    ):
        mock_post_repo = MagicMock()
        mock_post_repo.find_my_posts_by_cursor.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user
        mock_like_repo_cls.return_value = MagicMock()

        service = TechPostService(MagicMock())
        service.get_my_posts_by_cursor(1, None, None, 20)

        mock_post_repo.find_my_posts_by_cursor.assert_called_once_with(1, None, 20)

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_내_글_유효하지_않은_section이면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo

        service = TechPostService(MagicMock())

        with pytest.raises(InvalidSectionException):
            service.get_my_posts_by_cursor(1, "unknown", None, 20)

        mock_post_repo.find_my_posts_by_cursor.assert_not_called()

    @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    @patch("app.domain.pms.tech.service.TechPostRepository")
    def test_내_글_다른_섹션_section이면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        # 테이블 분리 후에는 tech 외 섹션이 존재하지 않으므로 invest도 유효하지 않은 섹션
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo

        service = TechPostService(MagicMock())

        with pytest.raises(InvalidSectionException):
            service.get_my_posts_by_cursor(1, "invest", None, 20)

        mock_post_repo.find_my_posts_by_cursor.assert_not_called()

    # offset 엔드포인트는 현재 비활성(router에서 주석)이라 학습용으로 테스트도 주석 처리
    # @patch("app.domain.pms.tech.service.LocalUmsApiClient")
    # @patch("app.domain.pms.tech.service.LocalCmsApiClient")
    # @patch("app.domain.pms.tech.service.TechPostCategoryMappingRepository")
    # @patch("app.domain.pms.tech.service.TechPostRepository")
    # def test_내_글_offset_목록과_페이지_메타를_반환(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
    #     mock_post_repo = MagicMock()
    #     mock_post_repo.count_my_posts.return_value = 3
    #     mock_post_repo.find_my_posts_by_offset.return_value = [_post_with_counts(3), _post_with_counts(2), _post_with_counts(1)]
    #     mock_post_repo_cls.return_value = mock_post_repo
    #     mock_pc_repo = MagicMock()
    #     mock_pc_repo.find_by_post_ids.return_value = []
    #     mock_pc_repo_cls.return_value = mock_pc_repo
    #     mock_user = MagicMock()
    #     mock_user.find_nicknames.return_value = {3: "me", 2: "me", 1: "me"}
    #     mock_user_cls.return_value = mock_user
    #
    #     service = TechPostService(MagicMock())
    #     result = service.get_my_posts_by_offset(1, None, 0, 20)
    #
    #     assert [p.id for p in result.data] == [3, 2, 1]
    #     assert result.total_count == 3
    #     assert result.total_pages == 1
    #     assert result.first is True
    #     assert result.last is True
