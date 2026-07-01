from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import TargetPostNotFoundException
from app.domain.comment.model import Comment
from app.domain.comment.schema import CreateCommentRequest
from app.domain.comment.service import CommentService


def _saved_comment(comment_id: int = 10) -> Comment:
    comment = Comment(post_id=1, user_id=1, content="내용")
    comment.id = comment_id
    return comment


class TestCommentService:

    @patch("app.domain.comment.service.LocalPostQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_정상_작성_시_댓글을_저장하고_id를_반환(self, mock_comment_repo_cls, mock_client_cls):
        mock_comment_repo = MagicMock()
        mock_comment_repo.save.return_value = _saved_comment(10)
        mock_comment_repo_cls.return_value = mock_comment_repo
        mock_client = MagicMock()
        mock_client.exists.return_value = True
        mock_client_cls.return_value = mock_client

        service = CommentService(MagicMock())
        result = service.create(1, 1, CreateCommentRequest(content="내용"))

        assert result.id == 10
        mock_comment_repo.save.assert_called_once()

    @patch("app.domain.comment.service.LocalPostQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_대상_글이_없으면_예외(self, mock_comment_repo_cls, mock_client_cls):
        mock_comment_repo = MagicMock()
        mock_comment_repo_cls.return_value = mock_comment_repo
        mock_client = MagicMock()
        mock_client.exists.return_value = False
        mock_client_cls.return_value = mock_client

        service = CommentService(MagicMock())

        with pytest.raises(TargetPostNotFoundException):
            service.create(99, 1, CreateCommentRequest(content="내용"))

        mock_comment_repo.save.assert_not_called()
