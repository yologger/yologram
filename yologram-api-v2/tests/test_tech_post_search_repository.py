from unittest.mock import MagicMock, patch

from app.domain.search.tech.model import TechPostSearchSort
from app.domain.search.tech.repository.tech_post_search_repository import (
    INDEX_ALIAS,
    TechPostSearchRepository,
)

PATCH_CLIENT = "app.domain.search.tech.repository.tech_post_search_repository.get_opensearch_client"


def _search(sort: TechPostSearchSort = TechPostSearchSort.RELEVANCE, hits: dict | None = None):
    """리포지토리로 질의하고 (결과, 전달된 body)를 반환"""
    with patch(PATCH_CLIENT) as mock_get_client:
        client = MagicMock()
        client.search.return_value = hits or {"hits": {"hits": [], "total": {"value": 0}}}
        mock_get_client.return_value = client

        result = TechPostSearchRepository().search("제미나이", from_=0, size=10, sort=sort)
        return result, client.search.call_args.kwargs


class TestQuery:

    def test_alias만_참조한다(self):
        # 실제 인덱스명(-v1)을 쓰면 v2 재색인 후 alias 이동으로 무중단 전환하는 전략이 깨진다
        _, kwargs = _search()

        assert kwargs["index"] == INDEX_ALIAS
        assert INDEX_ALIAS == "tech-post-index"

    def test_제목에_가중치를_주고_본문과_함께_검색한다(self):
        _, kwargs = _search()

        fields = kwargs["body"]["query"]["multi_match"]["fields"]
        assert fields == ["title^2", "content"]
        assert kwargs["body"]["query"]["multi_match"]["query"] == "제미나이"

    def test_from과_size를_그대로_전달한다(self):
        _, kwargs = _search()

        assert kwargs["body"]["from"] == 0
        assert kwargs["body"]["size"] == 10

    def test_track_total_hits를_켠다(self):
        # 기본값이면 10000건에서 카운트가 고정돼 마지막 페이지가 틀어진다
        _, kwargs = _search()

        assert kwargs["body"]["track_total_hits"] is True


class TestSort:

    def test_연관도순은_점수_먼저_동점이면_최신순(self):
        _, kwargs = _search(TechPostSearchSort.RELEVANCE)

        assert kwargs["body"]["sort"] == [
            {"_score": {"order": "desc"}},
            {"createdAt": {"order": "desc"}},
        ]

    def test_최신순은_시각_먼저_동시각이면_점수순(self):
        _, kwargs = _search(TechPostSearchSort.LATEST)

        assert kwargs["body"]["sort"] == [
            {"createdAt": {"order": "desc"}},
            {"_score": {"order": "desc"}},
        ]

    def test_어느_정렬이든_2차_키가_있다(self):
        # 1차 키 동점 시 순서가 흔들리면 페이징에서 중복·누락이 생긴다
        for sort in TechPostSearchSort:
            _, kwargs = _search(sort)
            assert len(kwargs["body"]["sort"]) == 2


class TestResultMapping:

    def test_응답의_source를_문서로_변환한다(self):
        hits = {
            "hits": {
                "total": {"value": 44},
                "hits": [
                    {
                        "_source": {
                            "id": 1200,
                            "uid": 12,
                            "title": "쿠쿠쿠",
                            "content": "쿠쿠쿠",
                            "categoryIds": [2],
                            "metrics": {"commentCount": 2, "likeCount": 1, "viewCount": 3},
                            "createdAt": "2026-07-18T14:23:50",
                        }
                    }
                ],
            }
        }
        result, _ = _search(hits=hits)

        assert result.total_count == 44
        doc = result.documents[0]
        # worker가 색인한 camelCase JSON을 snake_case 모델로 매핑한다
        assert doc.id == 1200
        assert doc.category_ids == [2]
        assert doc.metrics.comment_count == 2
        assert doc.metrics.view_count == 3

    def test_결과가_없으면_빈_목록과_0건(self):
        result, _ = _search()

        assert result.documents == []
        assert result.total_count == 0

    def test_total이_없어도_0으로_처리한다(self):
        result, _ = _search(hits={"hits": {"hits": []}})

        assert result.total_count == 0
