from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import (
    BlankSearchKeywordException,
    SearchPageTooDeepException,
    SearchUnavailableException,
)
from app.domain.search.tech.model import TechSearchSort
from app.domain.search.tech.news_document import TechNewsDocument
from app.domain.search.tech.news_search_service import TechNewsSearchService, MAX_PAGE_SIZE
from app.domain.search.tech.repository.tech_news_search_repository import TechNewsSearchResult

PATCH_SETTINGS = "app.domain.search.tech.news_search_service.get_settings"


# _service()가 시작한 patcher를 테스트 종료 시 정리한다
_ACTIVE_PATCHERS: list = []


def teardown_function():
    while _ACTIVE_PATCHERS:
        _ACTIVE_PATCHERS.pop().stop()


def _document(doc_id: int, category_ids: list[int] | None = None) -> TechNewsDocument:
    return TechNewsDocument(
        id=doc_id,
        title=f"제목 {doc_id}",
        summary=f"요약 {doc_id}",
        link=f"https://news.test/{doc_id}",
        source_name="GeekNews",
        category_ids=category_ids if category_ids is not None else [1],
        published_at=datetime(2026, 7, 18, 14, 23, 50),
    )


def _service(
    documents: list[TechNewsDocument] | None = None,
    total: int = 0,
    category_names: dict[int, str] | None = None,
    enabled: bool = True,
):
    """검색 리포지토리·cms를 목으로 바꾼 서비스와 목들을 함께 반환"""
    search_repo = MagicMock()
    search_repo.search.return_value = TechNewsSearchResult(
        documents=documents or [], total_count=total
    )
    cms = MagicMock()
    cms.find_category_names.return_value = category_names or {}

    service = TechNewsSearchService(
        MagicMock(), search_repository=search_repo, cms_api_client=cms
    )

    # 설정이 꺼져 있으면 503으로 끊기므로 기본은 켜둔다 (엔진에는 붙지 않는다 — 리포지토리가 목)
    settings_patcher = patch(PATCH_SETTINGS)
    mock_get_settings = settings_patcher.start()
    mock_get_settings.return_value = MagicMock(opensearch_main_enabled=enabled)
    _ACTIVE_PATCHERS.append(settings_patcher)

    return service, search_repo, cms


class TestAvailability:

    def test_검색_설정이_없으면_503_예외를_던지고_질의하지_않는다(self):
        # 조건부 라우터로 막으면 404가 되어 "없는 경로"로 오해된다 — 503으로 알린다
        service, search_repo, _ = _service(enabled=False)

        with pytest.raises(SearchUnavailableException):
            service.search("마이그레이션", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        search_repo.search.assert_not_called()


class TestValidation:

    def test_검색어가_비면_400_예외를_던지고_질의하지_않는다(self):
        service, search_repo, _ = _service()

        with pytest.raises(BlankSearchKeywordException):
            service.search("", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        search_repo.search.assert_not_called()

    def test_공백만_있는_검색어도_비어있는_것으로_본다(self):
        service, _, _ = _service()

        with pytest.raises(BlankSearchKeywordException):
            service.search("   ", page=0, size=10, sort=TechSearchSort.RELEVANCE)

    def test_검색어의_앞뒤_공백은_잘라서_질의한다(self):
        service, search_repo, _ = _service()

        service.search("  마이그레이션  ", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        assert search_repo.search.call_args.args[0] == "마이그레이션"

    def test_max_result_window를_넘는_페이지는_400_예외를_던진다(self):
        # 막지 않으면 OpenSearch가 예외를 내 500이 된다
        service, search_repo, _ = _service()

        with pytest.raises(SearchPageTooDeepException):
            service.search("마이그레이션", page=1000, size=10, sort=TechSearchSort.RELEVANCE)

        search_repo.search.assert_not_called()

    def test_한계_직전_페이지는_통과한다(self):
        service, search_repo, _ = _service(total=10_000)

        service.search("마이그레이션", page=999, size=10, sort=TechSearchSort.RELEVANCE)

        assert search_repo.search.call_args.kwargs["from_"] == 9990

    def test_size는_1에서_50으로_보정한다(self):
        service, search_repo, _ = _service()

        service.search("마이그레이션", page=0, size=999, sort=TechSearchSort.RELEVANCE)
        assert search_repo.search.call_args.kwargs["size"] == MAX_PAGE_SIZE

        service.search("마이그레이션", page=0, size=0, sort=TechSearchSort.RELEVANCE)
        assert search_repo.search.call_args.kwargs["size"] == 1

    def test_음수_페이지는_0으로_보정한다(self):
        service, search_repo, _ = _service()

        result = service.search("마이그레이션", page=-5, size=10, sort=TechSearchSort.RELEVANCE)

        assert search_repo.search.call_args.kwargs["from_"] == 0
        assert result.page == 0


class TestPaging:

    def test_총건수와_페이지_크기로_전체_페이지_수를_올림_계산한다(self):
        service, _, _ = _service(documents=[_document(1)], total=45)

        result = service.search("마이그레이션", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        assert result.total_count == 45
        assert result.total_pages == 5
        assert result.page == 0
        assert result.size == 10

    def test_첫_페이지는_first_마지막_페이지는_last(self):
        service, _, _ = _service(documents=[_document(1)], total=45)

        first = service.search("마이그레이션", page=0, size=10, sort=TechSearchSort.RELEVANCE)
        assert first.first and not first.last

        last = service.search("마이그레이션", page=4, size=10, sort=TechSearchSort.RELEVANCE)
        assert not last.first and last.last

    def test_결과가_없으면_총_0건이고_첫_페이지가_마지막이다(self):
        service, _, _ = _service(documents=[], total=0)

        result = service.search("없는키워드", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        assert result.total_count == 0
        assert result.total_pages == 0
        assert result.data == []
        assert result.first and result.last

    def test_페이지_번호가_from으로_변환된다(self):
        service, search_repo, _ = _service(total=100)

        service.search("마이그레이션", page=3, size=20, sort=TechSearchSort.RELEVANCE)

        assert search_repo.search.call_args.kwargs["from_"] == 60
        assert search_repo.search.call_args.kwargs["size"] == 20


class TestResponseAssembly:

    def test_색인_문서를_목록_응답_스키마로_변환한다(self):
        service, _, _ = _service(
            documents=[_document(900)], total=1, category_names={1: "인프라"}
        )

        result = service.search("마이그레이션", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        item = result.data[0]
        assert item.id == 900
        assert item.title == "제목 900"
        assert item.summary == "요약 900"
        assert item.link == "https://news.test/900"
        assert item.source_name == "GeekNews"
        assert item.categories == ["인프라"]
        assert item.published_at == datetime(2026, 7, 18, 14, 23, 50)

    def test_카테고리_라벨은_id를_모아_한_번만_조회한다(self):
        # 색인에 라벨을 넣지 않기로 했으므로(이름 변경 시 재색인 필요) 조회로 채운다 — N+1이 되면 안 된다
        docs = [
            _document(1, category_ids=[1, 2]),
            _document(2, category_ids=[2]),
            _document(3, category_ids=[1]),
        ]
        service, _, cms = _service(documents=docs, total=3, category_names={1: "인프라", 2: "AI"})

        service.search("마이그레이션", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        cms.find_category_names.assert_called_once_with({1, 2})

    def test_삭제된_카테고리는_라벨에서_빠진다(self):
        # 매핑은 남아 있는데 마스터에서 사라진 경우 — 목록 API와 같이 조용히 제외한다
        service, _, _ = _service(
            documents=[_document(1, category_ids=[1, 99])], total=1, category_names={1: "인프라"}
        )

        result = service.search("마이그레이션", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        assert result.data[0].categories == ["인프라"]

    def test_결과가_없으면_카테고리_조회도_빈_목록으로_나간다(self):
        service, _, cms = _service(documents=[], total=0)

        service.search("없는키워드", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        cms.find_category_names.assert_called_once_with(set())
