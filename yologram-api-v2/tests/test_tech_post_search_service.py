from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import BlankSearchKeywordException, SearchPageTooDeepException
from app.domain.search.tech.document import TechPostDocument, TechPostDocumentMetrics
from app.domain.search.tech.model import TechSearchSort
from app.domain.search.tech.repository.tech_post_search_repository import TechPostSearchResult
from app.domain.search.tech.post_search_service import TechPostSearchService, MAX_PAGE_SIZE

PATCH_REPO = "app.domain.search.tech.post_search_service.TechPostRepository"
PATCH_SETTINGS = "app.domain.search.tech.post_search_service.get_settings"


# _service()가 시작한 patcher를 테스트 종료 시 정리한다
_ACTIVE_PATCHERS: list = []


def teardown_function():
    while _ACTIVE_PATCHERS:
        _ACTIVE_PATCHERS.pop().stop()


def _document(doc_id: int, uid: int = 12) -> TechPostDocument:
    return TechPostDocument(
        id=doc_id,
        uid=uid,
        title=f"제목 {doc_id}",
        content=f"본문 {doc_id}",
        category_ids=[1],
        metrics=TechPostDocumentMetrics(comment_count=2, like_count=3, view_count=4),
        created_at=datetime(2026, 7, 18, 14, 23, 50),
    )


def _service(
    documents: list[TechPostDocument] | None = None,
    total: int = 0,
    liked_ids: set[int] | None = None,
    nicknames: dict[int, str] | None = None,
):
    """검색 리포지토리·ums·pms 리포지토리를 목으로 바꾼 서비스와 목들을 함께 반환"""
    search_repo = MagicMock()
    search_repo.search.return_value = TechPostSearchResult(
        documents=documents or [], total_count=total
    )
    ums = MagicMock()
    ums.find_nicknames.return_value = nicknames or {}

    with patch(PATCH_REPO) as mock_repo_cls:
        post_repo = MagicMock()
        post_repo.find_liked_post_ids.return_value = liked_ids or set()
        mock_repo_cls.return_value = post_repo
        service = TechPostSearchService(MagicMock(), search_repository=search_repo, ums_api_client=ums)

    # 설정이 꺼져 있으면 503으로 끊기므로 테스트에서는 켜둔다 (엔진에는 붙지 않는다 — 리포지토리가 목)
    settings_patcher = patch(PATCH_SETTINGS)
    mock_get_settings = settings_patcher.start()
    mock_get_settings.return_value = MagicMock(opensearch_main_enabled=True)
    _ACTIVE_PATCHERS.append(settings_patcher)

    return service, search_repo, ums, post_repo


class TestValidation:

    def test_검색어가_비면_400_예외를_던지고_질의하지_않는다(self):
        service, search_repo, _, _ = _service()

        with pytest.raises(BlankSearchKeywordException):
            service.search("", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        search_repo.search.assert_not_called()

    def test_공백만_있는_검색어도_비어있는_것으로_본다(self):
        service, _, _, _ = _service()

        with pytest.raises(BlankSearchKeywordException):
            service.search("   ", page=0, size=10, sort=TechSearchSort.RELEVANCE)

    def test_검색어의_앞뒤_공백은_잘라서_질의한다(self):
        service, search_repo, _, _ = _service()

        service.search("  제미나이  ", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        assert search_repo.search.call_args.args[0] == "제미나이"

    def test_max_result_window를_넘는_페이지는_400_예외를_던진다(self):
        # 막지 않으면 OpenSearch가 예외를 내 500이 된다
        service, search_repo, _, _ = _service()

        with pytest.raises(SearchPageTooDeepException):
            service.search("제미나이", page=1000, size=10, sort=TechSearchSort.RELEVANCE)

        search_repo.search.assert_not_called()

    def test_한계_직전_페이지는_통과한다(self):
        service, search_repo, _, _ = _service(total=10_000)

        service.search("제미나이", page=999, size=10, sort=TechSearchSort.RELEVANCE)

        assert search_repo.search.call_args.kwargs["from_"] == 9990

    def test_size는_1에서_50으로_보정한다(self):
        service, search_repo, _, _ = _service()

        service.search("제미나이", page=0, size=999, sort=TechSearchSort.RELEVANCE)
        assert search_repo.search.call_args.kwargs["size"] == MAX_PAGE_SIZE

        service.search("제미나이", page=0, size=0, sort=TechSearchSort.RELEVANCE)
        assert search_repo.search.call_args.kwargs["size"] == 1

    def test_음수_페이지는_0으로_보정한다(self):
        service, search_repo, _, _ = _service()

        result = service.search("제미나이", page=-5, size=10, sort=TechSearchSort.RELEVANCE)

        assert search_repo.search.call_args.kwargs["from_"] == 0
        assert result.page == 0


class TestPaging:

    def test_총건수와_페이지_크기로_전체_페이지_수를_올림_계산한다(self):
        service, _, _, _ = _service(documents=[_document(1)], total=45)

        result = service.search("제미나이", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        assert result.total_count == 45
        assert result.total_pages == 5
        assert result.page == 0
        assert result.size == 10

    def test_첫_페이지는_first_마지막_페이지는_last(self):
        service, _, _, _ = _service(documents=[_document(1)], total=45)

        first = service.search("제미나이", page=0, size=10, sort=TechSearchSort.RELEVANCE)
        assert first.first and not first.last

        last = service.search("제미나이", page=4, size=10, sort=TechSearchSort.RELEVANCE)
        assert not last.first and last.last

    def test_결과가_없으면_총_0건이고_첫_페이지가_마지막이다(self):
        service, _, _, _ = _service(documents=[], total=0)

        result = service.search("없는키워드", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        assert result.total_count == 0
        assert result.total_pages == 0
        assert result.data == []
        assert result.first and result.last

    def test_페이지_번호가_from으로_변환된다(self):
        service, search_repo, _, _ = _service(total=100)

        service.search("제미나이", page=3, size=20, sort=TechSearchSort.RELEVANCE)

        assert search_repo.search.call_args.kwargs["from_"] == 60
        assert search_repo.search.call_args.kwargs["size"] == 20


class TestResponseAssembly:

    def test_색인_문서를_목록_응답_스키마로_변환한다(self):
        service, _, _, _ = _service(
            documents=[_document(1200)], total=1, nicknames={12: "tester0"}
        )

        result = service.search("제미나이", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        item = result.data[0]
        assert item.id == 1200
        assert item.section == "TECH"
        assert item.author.uid == 12
        assert item.author.nickname == "tester0"
        assert item.title == "제목 1200"
        assert item.category_ids == [1]
        assert item.metrics.comment_count == 2
        assert item.metrics.like_count == 3
        assert item.metrics.view_count == 4
        assert item.created_at == datetime(2026, 7, 18, 14, 23, 50)

    def test_닉네임은_uid를_모아_한_번만_조회한다(self):
        # 색인에 닉네임을 넣지 않기로 했으므로(변경 시 재색인 필요) 조회로 채운다 — N+1이 되면 안 된다
        docs = [_document(1, uid=12), _document(2, uid=13), _document(3, uid=12)]
        service, _, ums, _ = _service(documents=docs, total=3, nicknames={12: "a", 13: "b"})

        service.search("제미나이", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        ums.find_nicknames.assert_called_once_with([12, 13, 12])

    def test_닉네임이_없으면_None으로_둔다(self):
        service, _, _, _ = _service(documents=[_document(1)], total=1, nicknames={})

        result = service.search("제미나이", page=0, size=10, sort=TechSearchSort.RELEVANCE)

        assert result.data[0].author.nickname is None


class TestPersonalization:

    def test_비로그인은_likedByMe가_False이고_이력을_조회하지_않는다(self):
        service, _, _, post_repo = _service(documents=[_document(1)], total=1)

        result = service.search("제미나이", page=0, size=10, sort=TechSearchSort.RELEVANCE, viewer_uid=None)

        assert result.data[0].metrics.liked_by_me is False
        post_repo.find_liked_post_ids.assert_not_called()

    def test_로그인_유저가_누른_글만_likedByMe가_True다(self):
        service, _, _, _ = _service(
            documents=[_document(1), _document(2)], total=2, liked_ids={1}
        )

        result = service.search("제미나이", page=0, size=10, sort=TechSearchSort.RELEVANCE, viewer_uid=12)

        by_id = {item.id: item for item in result.data}
        assert by_id[1].metrics.liked_by_me is True
        assert by_id[2].metrics.liked_by_me is False

    def test_결과가_비면_이력을_조회하지_않는다(self):
        service, _, _, post_repo = _service(documents=[], total=0)

        service.search("제미나이", page=0, size=10, sort=TechSearchSort.RELEVANCE, viewer_uid=12)

        post_repo.find_liked_post_ids.assert_not_called()


class TestSort:

    def test_정렬_기준을_그대로_리포지토리에_전달한다(self):
        service, search_repo, _, _ = _service()

        service.search("제미나이", page=0, size=10, sort=TechSearchSort.LATEST)

        assert search_repo.search.call_args.kwargs["sort"] is TechSearchSort.LATEST
