from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import InvalidSectionException
from app.domain.cms.enum import Section
from app.domain.cms.model import PostCategory
from app.domain.cms.service import PostCategoryService


class TestPostCategoryService:

    @patch("app.domain.cms.service.PostCategoryRepository")
    def test_section별_활성_카테고리를_정렬_순으로_반환(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active_by_section.return_value = [
            PostCategory(id=1, section=Section.TECH, name="Frontend", sort_order=1, is_active=True),
            PostCategory(id=2, section=Section.TECH, name="Backend", sort_order=2, is_active=True),
        ]
        mock_repo_cls.return_value = mock_repo

        service = PostCategoryService(MagicMock())
        result = service.get_post_categories("tech")

        assert len(result) == 2
        assert result[0].id == 1
        assert result[0].name == "Frontend"
        assert result[0].sort_order == 1
        assert result[1].name == "Backend"
        mock_repo.find_active_by_section.assert_called_once_with(Section.TECH)

    @patch("app.domain.cms.service.PostCategoryRepository")
    def test_대문자_section_path도_허용(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active_by_section.return_value = [
            PostCategory(id=8, section=Section.INVEST, name="국내주식", sort_order=1, is_active=True),
        ]
        mock_repo_cls.return_value = mock_repo

        service = PostCategoryService(MagicMock())
        result = service.get_post_categories("INVEST")

        assert len(result) == 1
        assert result[0].name == "국내주식"
        mock_repo.find_active_by_section.assert_called_once_with(Section.INVEST)

    @patch("app.domain.cms.service.PostCategoryRepository")
    def test_카테고리가_없으면_빈_목록_반환(self, mock_repo_cls):
        mock_repo = MagicMock()
        mock_repo.find_active_by_section.return_value = []
        mock_repo_cls.return_value = mock_repo

        service = PostCategoryService(MagicMock())
        result = service.get_post_categories("politics")

        assert result == []

    def test_유효하지_않은_section이면_예외(self):
        service = PostCategoryService(MagicMock())

        with pytest.raises(InvalidSectionException):
            service.get_post_categories("unknown")
