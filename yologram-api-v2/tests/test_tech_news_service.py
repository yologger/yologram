from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import InvalidCursorException
from app.domain.tech.article.cursor import TechArticleCursor
from app.domain.tech.article.model import TechArticle, TechArticleCategoryMapping, TechArticleStatus
from app.domain.tech.article.service import MAX_PAGE_SIZE, TechArticleService
from app.domain.tech.category.model import TechCategory


def _article(article_id: int, published_at: datetime = datetime(2026, 7, 18, 9, 0)) -> TechArticle:
    return TechArticle(
        id=article_id,
        source_id=1,
        title=f"제목 {article_id}",
        link=f"https://a/{article_id}",
        summary=f"요약 {article_id}",
        source_name="테크 블로그",
        published_at=published_at,
        status=TechArticleStatus.SUMMARIZED.value,
    )


def _mapping(mapping_id: int, article_id: int, category_id: int) -> TechArticleCategoryMapping:
    return TechArticleCategoryMapping(id=mapping_id, article_id=article_id, category_id=category_id)


def _category(category_id: int, name: str) -> TechCategory:
    return TechCategory(id=category_id, name=name, sort_order=category_id, is_active=True)


class TestTechArticleService:

    def setup_method(self):
        self.article_repo = MagicMock()
        self.mapping_repo = MagicMock()
        self.category_repo = MagicMock()
        self.mapping_repo.find_by_article_ids.return_value = []
        self.category_repo.find_by_ids.return_value = []

        self.repo_patcher = patch(
            "app.domain.tech.article.service.TechArticleRepository", return_value=self.article_repo
        )
        self.mapping_patcher = patch(
            "app.domain.tech.article.service.TechArticleCategoryMappingRepository", return_value=self.mapping_repo
        )
        self.category_patcher = patch(
            "app.domain.tech.article.service.TechCategoryRepository", return_value=self.category_repo
        )
        self.repo_patcher.start()
        self.mapping_patcher.start()
        self.category_patcher.start()
        self.service = TechArticleService(MagicMock())

    def teardown_method(self):
        self.repo_patcher.stop()
        self.mapping_patcher.stop()
        self.category_patcher.stop()

    def test_아티클_목록과_nextCursor를_반환한다(self):
        last = _article(1, datetime(2026, 7, 17, 9, 0))
        self.article_repo.find_summarized_articles.return_value = [_article(2), last]

        result = self.service.get_articles_by_cursor(category_id=None, cursor=None, size=20)

        assert len(result.data) == 2
        assert result.data[0].title == "제목 2"
        assert result.data[0].summary == "요약 2"
        assert result.next_cursor == TechArticleCursor.encode(last.published_at, last.id)

    def test_summary가_없으면_빈_문자열로_폴백된다(self):
        article = _article(1)
        article.summary = None
        self.article_repo.find_summarized_articles.return_value = [article]

        result = self.service.get_articles_by_cursor(category_id=None, cursor=None, size=20)

        assert result.data[0].summary == ""

    def test_카테고리가_배치_조회되어_tech_category_라벨로_해석된다(self):
        self.article_repo.find_summarized_articles.return_value = [_article(1), _article(2)]
        self.mapping_repo.find_by_article_ids.return_value = [
            _mapping(1, 1, 10),
            _mapping(2, 1, 20),
        ]
        self.category_repo.find_by_ids.return_value = [
            _category(10, "Backend"),
            _category(20, "Cloud"),
        ]

        result = self.service.get_articles_by_cursor(category_id=None, cursor=None, size=20)

        self.mapping_repo.find_by_article_ids.assert_called_once_with([1, 2])
        # 매핑의 category_id를 중복 없이 마스터에서 배치 해석 (N+1 회피)
        (called_ids,), _ = self.category_repo.find_by_ids.call_args
        assert sorted(called_ids) == [10, 20]
        assert result.data[0].categories == ["Backend", "Cloud"]
        assert result.data[1].categories == []  # 매핑 없는 글은 빈 목록

    def test_삭제된_카테고리_매핑은_라벨에서_제외된다(self):
        self.article_repo.find_summarized_articles.return_value = [_article(1)]
        self.mapping_repo.find_by_article_ids.return_value = [
            _mapping(1, 1, 10),
            _mapping(2, 1, 99),  # 마스터에서 삭제된 카테고리
        ]
        self.category_repo.find_by_ids.return_value = [_category(10, "Backend")]

        result = self.service.get_articles_by_cursor(category_id=None, cursor=None, size=20)

        assert result.data[0].categories == ["Backend"]

    def test_categoryId_필터가_리포지토리로_전달된다(self):
        self.article_repo.find_summarized_articles.return_value = []

        self.service.get_articles_by_cursor(category_id=2, cursor=None, size=20)

        self.article_repo.find_summarized_articles.assert_called_once_with(2, None, 20)

    def test_결과가_비면_nextCursor는_None이다(self):
        self.article_repo.find_summarized_articles.return_value = []

        result = self.service.get_articles_by_cursor(category_id=None, cursor=None, size=20)

        assert result.data == []
        assert result.next_cursor is None

    def test_커서가_디코딩되어_리포지토리로_전달된다(self):
        published_at = datetime(2026, 7, 18, 9, 0)
        cursor = TechArticleCursor.encode(published_at, 42)
        self.article_repo.find_summarized_articles.return_value = []

        self.service.get_articles_by_cursor(category_id=None, cursor=cursor, size=20)

        self.article_repo.find_summarized_articles.assert_called_once_with(
            None, TechArticleCursor(published_at=published_at, id=42), 20
        )

    def test_잘못된_커서면_INVALID_CURSOR_예외가_발생한다(self):
        with pytest.raises(InvalidCursorException):
            self.service.get_articles_by_cursor(category_id=None, cursor="@@@", size=20)

        self.article_repo.find_summarized_articles.assert_not_called()

    def test_size는_1에서_50으로_보정된다(self):
        self.article_repo.find_summarized_articles.return_value = []

        self.service.get_articles_by_cursor(category_id=None, cursor=None, size=999)
        self.article_repo.find_summarized_articles.assert_called_with(None, None, MAX_PAGE_SIZE)

        self.service.get_articles_by_cursor(category_id=None, cursor=None, size=-1)
        self.article_repo.find_summarized_articles.assert_called_with(None, None, 1)
