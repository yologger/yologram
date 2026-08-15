from unittest.mock import MagicMock, patch

import pytest

from app.core.exception import InvalidIndexRangeException
from app.domain.search.tech.publisher.message.tech_indexing_message import TARGET_TECH_POST
from app.domain.search.tech.post_indexing_service import AdminTechPostIndexingService

PATCH_REPO = "app.domain.search.tech.post_indexing_service.TechPostRepository"


def _service(publisher: MagicMock, max_id: int | None = None):
    """리포지토리를 목으로 바꾼 서비스 — publisher는 호출부에서 주입"""
    with patch(PATCH_REPO) as mock_repo_cls:
        mock_repo = MagicMock()
        mock_repo.find_max_id.return_value = max_id
        mock_repo_cls.return_value = mock_repo
        return AdminTechPostIndexingService(MagicMock(), publisher=publisher)


def _published_ranges(publisher: MagicMock) -> list[tuple[int, int]]:
    return [(c.args[0].from_id, c.args[0].to_id) for c in publisher.publish.call_args_list]


class TestIndexSingle:

    def test_from과_to를_같은_값으로_발행한다(self):
        publisher = MagicMock()
        _service(publisher).index(42)

        assert _published_ranges(publisher) == [(42, 42)]
        assert publisher.publish.call_args.args[0].target == TARGET_TECH_POST


class TestIndexRange:

    def test_청크_크기_이하_범위는_한_건만_발행한다(self):
        publisher = MagicMock()
        published = _service(publisher).index_range(1, 20)

        assert published == 1
        assert _published_ranges(publisher) == [(1, 20)]

    def test_청크_경계를_넘으면_나눠_발행한다(self):
        publisher = MagicMock()
        published = _service(publisher).index_range(1, 45)

        assert published == 3
        # 마지막 청크는 to에서 끊긴다 — 범위 밖 id를 조회하지 않는다
        assert _published_ranges(publisher) == [(1, 20), (21, 40), (41, 45)]

    def test_청크로_정확히_나눠떨어지면_빈_메시지를_더_만들지_않는다(self):
        publisher = MagicMock()
        published = _service(publisher).index_range(1, 40)

        assert published == 2
        assert _published_ranges(publisher) == [(1, 20), (21, 40)]

    def test_from과_to가_같으면_한_건만_발행한다(self):
        publisher = MagicMock()
        published = _service(publisher).index_range(7, 7)

        assert published == 1
        assert _published_ranges(publisher) == [(7, 7)]

    def test_1에서_시작하지_않는_범위도_그대로_쪼갠다(self):
        publisher = MagicMock()
        published = _service(publisher).index_range(100, 130)

        assert published == 2
        assert _published_ranges(publisher) == [(100, 119), (120, 130)]

    def test_from이_to보다_크면_발행하지_않고_예외를_던진다(self):
        publisher = MagicMock()

        with pytest.raises(InvalidIndexRangeException):
            _service(publisher).index_range(30, 10)

        publisher.publish.assert_not_called()

    def test_id는_1부터라_0_이하로_시작하는_범위는_예외를_던진다(self):
        publisher = MagicMock()

        with pytest.raises(InvalidIndexRangeException):
            _service(publisher).index_range(0, 10)

        publisher.publish.assert_not_called()


class TestFullIndex:

    def test_1부터_max_id까지_쪼개_발행한다(self):
        publisher = MagicMock()
        published = _service(publisher, max_id=45).full_index()

        assert published == 3
        assert _published_ranges(publisher) == [(1, 20), (21, 40), (41, 45)]

    def test_글이_하나도_없으면_발행하지_않는다(self):
        publisher = MagicMock()
        published = _service(publisher, max_id=None).full_index()

        assert published == 0
        publisher.publish.assert_not_called()

    def test_max_id가_0이면_발행하지_않는다(self):
        publisher = MagicMock()
        published = _service(publisher, max_id=0).full_index()

        assert published == 0
        publisher.publish.assert_not_called()

    def test_글이_하나뿐이면_한_건만_발행한다(self):
        publisher = MagicMock()
        published = _service(publisher, max_id=1).full_index()

        assert published == 1
        assert _published_ranges(publisher) == [(1, 1)]

    def test_삭제로_id에_공백이_있어도_max_id까지_전부_훑는다(self):
        # 삭제된 id 구간은 워커가 조회 0건으로 흘려보낸다 — 발행 단계에서 걸러내지 않는다
        publisher = MagicMock()
        published = _service(publisher, max_id=21).full_index()

        assert published == 2
        assert _published_ranges(publisher) == [(1, 20), (21, 21)]


class TestFullIndexInBackground:

    def test_비동기_진입점도_같은_범위를_발행한다(self):
        publisher = MagicMock()
        _service(publisher, max_id=45).full_index_in_background()

        assert _published_ranges(publisher) == [(1, 20), (21, 40), (41, 45)]

    def test_비동기_진입점은_발행_실패를_삼킨다(self):
        # BackgroundTasks는 예외를 호출자에게 전달할 수 없다 — 밖으로 던지면 응답 이후에 터진다
        publisher = MagicMock()
        publisher.publish.side_effect = RuntimeError("sqs down")

        _service(publisher, max_id=45).full_index_in_background()

    def test_동기_full_index는_실패를_전파한다(self):
        # 비동기 래퍼만 삼킨다 — 안쪽은 그대로 두어 다른 호출자가 실패를 알 수 있게 한다
        publisher = MagicMock()
        publisher.publish.side_effect = RuntimeError("sqs down")

        with pytest.raises(RuntimeError):
            _service(publisher, max_id=45).full_index()
