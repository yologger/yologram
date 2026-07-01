import base64
from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import InvalidCursorException, TargetPostNotFoundException
from app.domain.comment.model import Comment
from app.domain.comment.schema import CreateCommentRequest
from app.domain.comment.service import CommentService
from app.domain.comment.sort import CommentSort


def _saved_comment(comment_id: int = 10) -> Comment:
    comment = Comment(post_id=1, user_id=1, content="내용")
    comment.id = comment_id
    return comment


def _comment(comment_id: int, post_id: int = 1, user_id: int = 1) -> Comment:
    comment = Comment(post_id=post_id, user_id=user_id, content="내용")
    comment.id = comment_id
    comment.created_at = datetime(2026, 1, 1)
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


class TestCommentServiceQuery:

    @patch("app.domain.comment.service.LocalUserQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_cursor_조회_정상_시_nextCursor는_마지막_id(self, mock_repo_cls, mock_user_cls):
        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = [_comment(5), _comment(3)]
        mock_repo_cls.return_value = mock_repo
        mock_user = MagicMock()
        mock_user.find_nicknames.return_value = {1: "닉네임"}
        mock_user_cls.return_value = mock_user

        service = CommentService(MagicMock())
        result = service.get_comments_by_cursor(1, None, None, 20)

        assert len(result.data) == 2
        assert result.data[0].id == 5
        assert result.data[0].post_id == 1
        assert result.data[0].author.nickname == "닉네임"
        expected = base64.urlsafe_b64encode(b"3").decode().rstrip("=")
        assert result.next_cursor == expected

    @patch("app.domain.comment.service.LocalUserQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_cursor_빈_목록이면_nextCursor_None(self, mock_repo_cls, mock_user_cls):
        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        service = CommentService(MagicMock())
        result = service.get_comments_by_cursor(999, None, None, 20)

        assert result.data == []
        assert result.next_cursor is None

    @patch("app.domain.comment.service.LocalUserQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_cursor_전달_시_디코딩되어_repository로(self, mock_repo_cls, mock_user_cls):
        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        cursor = base64.urlsafe_b64encode(b"10").decode().rstrip("=")
        service = CommentService(MagicMock())
        service.get_comments_by_cursor(1, None, cursor, 20)

        args = mock_repo.find_by_post_cursor.call_args[0]
        assert args[2] == 10  # cursor_id

    @patch("app.domain.comment.service.LocalUserQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_sort_oldest_시_OLDEST_전달(self, mock_repo_cls, mock_user_cls):
        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        service = CommentService(MagicMock())
        service.get_comments_by_cursor(1, "oldest", None, 20)

        args = mock_repo.find_by_post_cursor.call_args[0]
        assert args[1] == CommentSort.OLDEST

    @patch("app.domain.comment.service.LocalUserQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_size_최대_50_제한(self, mock_repo_cls, mock_user_cls):
        mock_repo = MagicMock()
        mock_repo.find_by_post_cursor.return_value = []
        mock_repo_cls.return_value = mock_repo
        mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))

        service = CommentService(MagicMock())
        service.get_comments_by_cursor(1, None, None, 100)

        args = mock_repo.find_by_post_cursor.call_args[0]
        assert args[3] == 50  # limit

    @patch("app.domain.comment.service.LocalUserQueryClient")
    @patch("app.domain.comment.service.CommentRepository")
    def test_잘못된_커서면_InvalidCursorException(self, mock_repo_cls, mock_user_cls):
        mock_repo_cls.return_value = MagicMock()
        mock_user_cls.return_value = MagicMock()

        service = CommentService(MagicMock())

        with pytest.raises(InvalidCursorException):
            service.get_comments_by_cursor(1, None, "!!!invalid!!!", 20)

    # --- offset 조회 (학습용) — 엔드포인트 비활성(router 주석)이므로 테스트도 주석 처리, 코드는 보존 ---
    # @patch("app.domain.comment.service.LocalUserQueryClient")
    # @patch("app.domain.comment.service.CommentRepository")
    # def test_offset_조회_시_page_정보_반환(self, mock_repo_cls, mock_user_cls):
    #     mock_repo = MagicMock()
    #     mock_repo.count_by_post.return_value = 5
    #     mock_repo.find_by_post_offset.return_value = [_comment(2), _comment(1)]
    #     mock_repo_cls.return_value = mock_repo
    #     mock_user_cls.return_value = MagicMock(find_nicknames=MagicMock(return_value={}))
    #
    #     service = CommentService(MagicMock())
    #     result = service.get_comments_by_offset(1, None, 0, 2)
    #
    #     assert result.total_count == 5
    #     assert result.total_pages == 3
    #     assert result.first is True
    #     assert result.last is False
    #     assert len(result.data) == 2
