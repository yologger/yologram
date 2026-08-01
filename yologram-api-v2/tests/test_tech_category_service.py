from unittest.mock import MagicMock, patch

from app.domain.cms.tech.model import TechCategory
from app.domain.cms.tech.service import TechCategoryService


class TestTechCategoryService:

    @patch("app.domain.cms.tech.service.TechCategoryRepository")
    def test_활성_카테고리를_정렬_순으로_반환(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active.return_value = [
            TechCategory(id=1, name="Frontend", sort_order=1, is_active=True),
            TechCategory(id=2, name="Backend", sort_order=2, is_active=True),
        ]
        mock_repo_cls.return_value = mock_repo

        service = TechCategoryService(MagicMock())
        result = service.get_categories()

        assert len(result) == 2
        assert result[0].id == 1
        assert result[0].name == "Frontend"
        assert result[0].sort_order == 1
        assert result[1].name == "Backend"
        mock_repo.find_active.assert_called_once_with()

    @patch("app.domain.cms.tech.service.TechCategoryRepository")
    def test_카테고리가_없으면_빈_목록_반환(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active.return_value = []
        mock_repo_cls.return_value = mock_repo

        service = TechCategoryService(MagicMock())
        result = service.get_categories()

        assert result == []
