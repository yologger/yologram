from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import PostNotFoundException
from app.domain.pms.tech.like_service import TechPostLikeService
from app.domain.pms.tech.model import TechPost


def _post(post_id: int = 1) -> TechPost:
    post = TechPost(user_id=1, content="내용")
    post.id = post_id
    return post


class TestTechPostLikeServiceLike:

    @patch("app.domain.pms.tech.like_service.TechPostLikeCountRepository")
    @patch("app.domain.pms.tech.like_service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.like_service.TechPostRepository")
    def test_처음_좋아요면_이력_삽입_후_카운트_증가(self, mock_post_repo_cls, mock_like_repo_cls, mock_count_repo_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = _post(1)
        mock_post_repo_cls.return_value = mock_post_repo
        mock_like_repo = MagicMock()
        mock_like_repo.insert_ignore.return_value = 1  # 실제 삽입됨
        mock_like_repo_cls.return_value = mock_like_repo
        mock_count_repo = MagicMock()
        mock_count_repo_cls.return_value = mock_count_repo

        service = TechPostLikeService(MagicMock())
        service.like(1, 7)

        mock_like_repo.insert_ignore.assert_called_once_with(1, 7)
        mock_count_repo.increase.assert_called_once_with(1)

    @patch("app.domain.pms.tech.like_service.TechPostLikeCountRepository")
    @patch("app.domain.pms.tech.like_service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.like_service.TechPostRepository")
    def test_이미_좋아요한_상태면_카운트_증가_생략_멱등(self, mock_post_repo_cls, mock_like_repo_cls, mock_count_repo_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = _post(1)
        mock_post_repo_cls.return_value = mock_post_repo
        mock_like_repo = MagicMock()
        mock_like_repo.insert_ignore.return_value = 0  # uk 충돌 무시 — 0행 삽입
        mock_like_repo_cls.return_value = mock_like_repo
        mock_count_repo = MagicMock()
        mock_count_repo_cls.return_value = mock_count_repo

        service = TechPostLikeService(MagicMock())
        service.like(1, 7)  # 예외 없이 no-op

        mock_count_repo.increase.assert_not_called()

    @patch("app.domain.pms.tech.like_service.TechPostLikeCountRepository")
    @patch("app.domain.pms.tech.like_service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.like_service.TechPostRepository")
    def test_없는_글이면_404_이력_삽입_미호출(self, mock_post_repo_cls, mock_like_repo_cls, mock_count_repo_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo
        mock_like_repo = MagicMock()
        mock_like_repo_cls.return_value = mock_like_repo
        mock_count_repo = MagicMock()
        mock_count_repo_cls.return_value = mock_count_repo

        service = TechPostLikeService(MagicMock())

        with pytest.raises(PostNotFoundException):
            service.like(999, 7)

        mock_like_repo.insert_ignore.assert_not_called()
        mock_count_repo.increase.assert_not_called()


class TestTechPostLikeServiceUnlike:

    @patch("app.domain.pms.tech.like_service.TechPostLikeCountRepository")
    @patch("app.domain.pms.tech.like_service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.like_service.TechPostRepository")
    def test_좋아요_상태면_이력_삭제_후_카운트_감소(self, mock_post_repo_cls, mock_like_repo_cls, mock_count_repo_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = _post(1)
        mock_post_repo_cls.return_value = mock_post_repo
        mock_like_repo = MagicMock()
        mock_like_repo.delete_by_post_id_and_uid.return_value = 1  # 실제 삭제됨
        mock_like_repo_cls.return_value = mock_like_repo
        mock_count_repo = MagicMock()
        mock_count_repo_cls.return_value = mock_count_repo

        service = TechPostLikeService(MagicMock())
        service.unlike(1, 7)

        mock_like_repo.delete_by_post_id_and_uid.assert_called_once_with(1, 7)
        mock_count_repo.decrease.assert_called_once_with(1)

    @patch("app.domain.pms.tech.like_service.TechPostLikeCountRepository")
    @patch("app.domain.pms.tech.like_service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.like_service.TechPostRepository")
    def test_안_누른_상태면_카운트_감소_생략_멱등(self, mock_post_repo_cls, mock_like_repo_cls, mock_count_repo_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = _post(1)
        mock_post_repo_cls.return_value = mock_post_repo
        mock_like_repo = MagicMock()
        mock_like_repo.delete_by_post_id_and_uid.return_value = 0  # 지울 게 없음
        mock_like_repo_cls.return_value = mock_like_repo
        mock_count_repo = MagicMock()
        mock_count_repo_cls.return_value = mock_count_repo

        service = TechPostLikeService(MagicMock())
        service.unlike(1, 7)  # 예외 없이 no-op

        mock_count_repo.decrease.assert_not_called()

    @patch("app.domain.pms.tech.like_service.TechPostLikeCountRepository")
    @patch("app.domain.pms.tech.like_service.TechPostLikeRepository")
    @patch("app.domain.pms.tech.like_service.TechPostRepository")
    def test_없는_글이면_404_이력_삭제_미호출(self, mock_post_repo_cls, mock_like_repo_cls, mock_count_repo_cls):
        mock_post_repo = MagicMock()
        mock_post_repo.find_by_id.return_value = None
        mock_post_repo_cls.return_value = mock_post_repo
        mock_like_repo = MagicMock()
        mock_like_repo_cls.return_value = mock_like_repo
        mock_count_repo = MagicMock()
        mock_count_repo_cls.return_value = mock_count_repo

        service = TechPostLikeService(MagicMock())

        with pytest.raises(PostNotFoundException):
            service.unlike(999, 7)

        mock_like_repo.delete_by_post_id_and_uid.assert_not_called()
        mock_count_repo.decrease.assert_not_called()
