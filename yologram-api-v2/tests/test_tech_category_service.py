from unittest.mock import MagicMock, patch

from app.domain.tech.category.model import TechPostCategory
from app.domain.tech.category.service import TechPostCategoryService


class TestTechPostCategoryService:

    @patch("app.domain.tech.category.service.TechPostCategoryRepository")
    def test_활성_카테고리를_정렬_순으로_반환(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active.return_value = [
            TechPostCategory(id=1, name="Frontend", sort_order=1, is_active=True),
            TechPostCategory(id=2, name="Backend", sort_order=2, is_active=True),
        ]
        mock_repo_cls.return_value = mock_repo

        service = TechPostCategoryService(MagicMock())
        result = service.get_post_categories()

        assert len(result) == 2
        assert result[0].id == 1
        assert result[0].name == "Frontend"
        assert result[0].sort_order == 1
        assert result[1].name == "Backend"
        mock_repo.find_active.assert_called_once_with()

    @patch("app.domain.tech.category.service.TechPostCategoryRepository")
    def test_카테고리가_없으면_빈_목록_반환(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active.return_value = []
        mock_repo_cls.return_value = mock_repo

        service = TechPostCategoryService(MagicMock())
        result = service.get_post_categories()

        assert result == []
