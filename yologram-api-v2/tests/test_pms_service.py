from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import (
    InvalidPostCategoryException,
    InvalidSectionException,
    PostForbiddenException,
    PostNotFoundException,
)
from app.domain.cms.enum import Section
from app.domain.pms.cursor import PostCursor
from app.domain.pms.model import Post, PostCategoryMapping
from app.domain.pms.schema import CreatePostRequest, UpdatePostRequest
from app.domain.pms.service import PostService


def _saved_post(post_id: int = 10) -> Post:
    post = Post(section=Section.TECH, user_id=1, content="내용")
    post.id = post_id
    return post


def _post(post_id: int, user_id: int | None = None) -> Post:
    post = Post(section=Section.TECH, user_id=user_id or post_id, content=f"내용{post_id}")
    post.id = post_id
    post.like_count = 0
    post.comment_count = 0
    post.created_at = datetime(2026, 1, 1, 0, 0)
    return post


class TestPostService:

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_정상_작성_시_게시글과_카테고리를_저장하고_id를_반환(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.save.return_value = _saved_post(10)
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_client = MagicMock()
        mock_client.all_active_in_section.return_value = True
        mock_client_cls.return_value = mock_client

        service = PostService(MagicMock())
        result = service.create("tech", 1, CreatePostRequest(content="내용", category_ids=[1, 2]))

        assert result.id == 10
        assert mock_pc_repo.save.call_count == 2

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_카테고리가_해당_section_것이_아니면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo
        mock_client = MagicMock()
        mock_client.all_active_in_section.return_value = False
        mock_client_cls.return_value = mock_client

        service = PostService(MagicMock())

        with pytest.raises(InvalidPostCategoryException):
            service.create("tech", 1, CreatePostRequest(content="내용", category_ids=[99]))

        mock_post_repo.save.assert_not_called()

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_유효하지_않은_section이면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo

        service = PostService(MagicMock())

        with pytest.raises(InvalidSectionException):
            service.create("unknown", 1, CreatePostRequest(content="내용", category_ids=[1]))

        mock_post_repo.save.assert_not_called()


class TestPostServiceUpdate:

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_본인_글이면_수정_후_카테고리_교체(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        post = _post(1, user_id=1)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat = MagicMock()
        mock_cat.all_active_in_section.return_value = True
        mock_cat_cls.return_value = mock_cat

        service = PostService(MagicMock())
        service.update("tech", 1, 1, UpdatePostRequest(title="새 제목", content="새 내용", category_ids=[2, 3]))

        assert post.title == "새 제목"
        assert post.content == "새 내용"
        mock_pc_repo.delete_by_post_id.assert_called_once_with(1)
        assert mock_pc_repo.save.call_count == 2

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_존재하지_않는_글이면_404(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat_cls.return_value = MagicMock()

        service = PostService(MagicMock())
        with pytest.raises(PostNotFoundException):
            service.update("tech", 99, 1, UpdatePostRequest(content="내용", category_ids=[1]))

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_다른_section의_글이면_404(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        post = Post(section=Section.INVEST, user_id=1, content="내용")
        post.id = 1
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo_cls.return_value = MagicMock()
        mock_cat_cls.return_value = MagicMock()

        service = PostService(MagicMock())
        with pytest.raises(PostNotFoundException):
            service.update("tech", 1, 1, UpdatePostRequest(content="내용", category_ids=[1]))

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_본인_글이_아니면_403(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        post = _post(1, user_id=99)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat_cls.return_value = MagicMock()

        service = PostService(MagicMock())
        with pytest.raises(PostForbiddenException):
            service.update("tech", 1, 1, UpdatePostRequest(content="내용", category_ids=[1]))

        mock_pc_repo.delete_by_post_id.assert_not_called()

    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_카테고리가_해당_section_것이_아니면_400(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls):
        post = _post(1, user_id=1)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_cat = MagicMock()
        mock_cat.all_active_in_section.return_value = False
        mock_cat_cls.return_value = mock_cat

        service = PostService(MagicMock())
        with pytest.raises(InvalidPostCategoryException):
            service.update("tech", 1, 1, UpdatePostRequest(content="내용", category_ids=[99]))

        mock_pc_repo.delete_by_post_id.assert_not_called()


class TestPostServiceGetPost:

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_게시글과_카테고리_작성자_닉네임을_반환(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        post = Post(section=Section.TECH, user_id=12, title="제목", content="내용")
        post.id = 1
        post.like_count = 3
        post.comment_count = 2
        post.created_at = datetime(2026, 1, 1, 0, 0)
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_id.return_value = [
            PostCategoryMapping(post_id=1, category_id=1),
            PostCategoryMapping(post_id=1, category_id=2),
        ]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nickname.return_value = "tester"
        mock_user_cls.return_value = mock_user

        service = PostService(MagicMock())
        result = service.get_post("tech", 1)

        assert result.id == 1
        assert result.author.uid == 12
        assert result.author.nickname == "tester"
        assert result.category_ids == [1, 2]
        assert result.content == "내용"
        assert result.comment_count == 2

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_존재하지_않는_게시글이면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo

        service = PostService(MagicMock())

        with pytest.raises(PostNotFoundException):
            service.get_post("tech", 99)

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_id가_해당_section_글이_아니면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        post = Post(section=Section.INVEST, user_id=12, content="내용")
        post.id = 1
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = post
        mock_post_repo_cls.return_value = mock_post_repo

        service = PostService(MagicMock())

        with pytest.raises(PostNotFoundException):
            service.get_post("tech", 1)


class TestPostServiceGetPosts:

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_결과가_있으면_마지막_글_id를_nextCursor로_반환(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts_by_section.return_value = [_post(3), _post(2)]
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = [PostCategoryMapping(post_id=3, category_id=10)]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {3: "u3", 2: "u2"}
        mock_user_cls.return_value = mock_user

        service = PostService(MagicMock())
        result = service.get_posts_by_cursor("tech", None, None, 2)

        assert [p.id for p in result.data] == [3, 2]
        assert result.data[0].category_ids == [10]
        assert result.data[0].author.nickname == "u3"
        assert result.next_cursor == PostCursor.encode(2)

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_결과가_없으면_빈_목록과_None_nextCursor(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts_by_section.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user

        service = PostService(MagicMock())
        result = service.get_posts_by_cursor("tech", None, None, 20)

        assert result.data == []
        assert result.next_cursor is None

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_cursor가_주어지면_디코딩한_id로_조회(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts_by_section.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user

        service = PostService(MagicMock())
        service.get_posts_by_cursor("tech", None, PostCursor.encode(5), 20)

        mock_post_repo.find_posts_by_section.assert_called_once_with(Section.TECH, None, 5, 20)

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_size가_최대치를_넘으면_50으로_제한(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_posts_by_section.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user

        service = PostService(MagicMock())
        service.get_posts_by_cursor("tech", None, None, 100)

        mock_post_repo.find_posts_by_section.assert_called_once_with(Section.TECH, None, None, 50)

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_유효하지_않은_section이면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo

        service = PostService(MagicMock())

        with pytest.raises(InvalidSectionException):
            service.get_posts_by_cursor("unknown", None, None, 20)

        mock_post_repo.find_posts_by_section.assert_not_called()


class TestPostServiceGetMyPosts:

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_내_글_cursor_목록과_nextCursor를_반환(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_my_posts_by_cursor.return_value = [_post(3), _post(2)]
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = [PostCategoryMapping(post_id=3, category_id=10)]
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {3: "me", 2: "me"}
        mock_user_cls.return_value = mock_user

        service = PostService(MagicMock())
        result = service.get_my_posts_by_cursor(1, None, None, 2)

        assert [p.id for p in result.data] == [3, 2]
        assert result.data[0].category_ids == [10]
        assert result.next_cursor == PostCursor.encode(2)

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_내_글_section_지정시_해당_section으로_조회(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_my_posts_by_cursor.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user

        service = PostService(MagicMock())
        service.get_my_posts_by_cursor(1, "invest", None, 20)

        mock_post_repo.find_my_posts_by_cursor.assert_called_once_with(1, Section.INVEST, None, 20)

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_내_글_section_없으면_전체_조회(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_my_posts_by_cursor.return_value = []
        mock_post_repo_cls.return_value = mock_post_repo
        mock_pc_repo = MagicMock()
        mock_pc_repo.find_by_post_ids.return_value = []
        mock_pc_repo_cls.return_value = mock_pc_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {}
        mock_user_cls.return_value = mock_user

        service = PostService(MagicMock())
        service.get_my_posts_by_cursor(1, None, None, 20)

        mock_post_repo.find_my_posts_by_cursor.assert_called_once_with(1, None, None, 20)

    @patch("app.domain.pms.service.LocalUserQueryClient")
    @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryMappingRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_내_글_유효하지_않은_section이면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo

        service = PostService(MagicMock())

        with pytest.raises(InvalidSectionException):
            service.get_my_posts_by_cursor(1, "unknown", None, 20)

        mock_post_repo.find_my_posts_by_cursor.assert_not_called()

    # offset 엔드포인트는 현재 비활성(router에서 주석)이라 학습용으로 테스트도 주석 처리
    # @patch("app.domain.pms.service.LocalUserQueryClient")
    # @patch("app.domain.pms.service.LocalPostCategoryQueryClient")
    # @patch("app.domain.pms.service.PostCategoryMappingRepository")
    # @patch("app.domain.pms.service.PostRepository")
    # def test_내_글_offset_목록과_페이지_메타를_반환(self, mock_post_repo_cls, mock_pc_repo_cls, mock_cat_cls, mock_user_cls):
    #     mock_post_repo = MagicMock()
    #     mock_post_repo.count_my_posts.return_value = 3
    #     mock_post_repo.find_my_posts_by_offset.return_value = [_post(3), _post(2), _post(1)]
    #     mock_post_repo_cls.return_value = mock_post_repo
    #     mock_pc_repo = MagicMock()
    #     mock_pc_repo.find_by_post_ids.return_value = []
    #     mock_pc_repo_cls.return_value = mock_pc_repo
    #     mock_user = MagicMock()
    #     mock_user.find_nicknames.return_value = {3: "me", 2: "me", 1: "me"}
    #     mock_user_cls.return_value = mock_user
    #
    #     service = PostService(MagicMock())
    #     result = service.get_my_posts_by_offset(1, None, 0, 20)
    #
    #     assert [p.id for p in result.data] == [3, 2, 1]
    #     assert result.total_count == 3
    #     assert result.total_pages == 1
    #     assert result.first is True
    #     assert result.last is True
