from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import InvalidCategoryException, InvalidSectionException
from app.domain.cms.enum import Section
from app.domain.pms.model import Post
from app.domain.pms.schema import CreatePostRequest
from app.domain.pms.service import PostService


def _saved_post(post_id: int = 10) -> Post:
    post = Post(section=Section.TECH, user_id=1, content="내용")
    post.id = post_id
    return post


class TestPostService:

    @patch("app.domain.pms.service.LocalCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryRepository")
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

    @patch("app.domain.pms.service.LocalCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_카테고리가_해당_section_것이_아니면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo
        mock_client = MagicMock()
        mock_client.all_active_in_section.return_value = False
        mock_client_cls.return_value = mock_client

        service = PostService(MagicMock())

        with pytest.raises(InvalidCategoryException):
            service.create("tech", 1, CreatePostRequest(content="내용", category_ids=[99]))

        mock_post_repo.save.assert_not_called()

    @patch("app.domain.pms.service.LocalCategoryQueryClient")
    @patch("app.domain.pms.service.PostCategoryRepository")
    @patch("app.domain.pms.service.PostRepository")
    def test_유효하지_않은_section이면_예외(self, mock_post_repo_cls, mock_pc_repo_cls, mock_client_cls):
        mock_post_repo = MagicMock()
        mock_post_repo_cls.return_value = mock_post_repo

        service = PostService(MagicMock())

        with pytest.raises(InvalidSectionException):
            service.create("unknown", 1, CreatePostRequest(content="내용", category_ids=[1]))

        mock_post_repo.save.assert_not_called()
