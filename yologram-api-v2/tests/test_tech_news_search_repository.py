from unittest.mock import MagicMock, patch

from app.domain.search.tech.model import TechSearchSort
from app.domain.search.tech.repository.tech_news_search_repository import (
    INDEX_ALIAS,
    TechNewsSearchRepository,
)

PATCH_CLIENT = "app.domain.search.tech.repository.tech_news_search_repository.get_opensearch_client"


def _search(sort: TechSearchSort = TechSearchSort.RELEVANCE, hits: dict | None = None):
    """리포지토리로 질의하고 (결과, 전달된 body)를 반환"""
    with patch(PATCH_CLIENT) as mock_get_client:
        client = MagicMock()
        client.search.return_value = hits or {"hits": {"hits": [], "total": {"value": 0}}}
        mock_get_client.return_value = client

        result = TechNewsSearchRepository().search("마이그레이션", from_=0, size=10, sort=sort)
        return result, client.search.call_args.kwargs


class TestQuery:

    def test_alias만_참조한다(self):
        # 실제 인덱스명(-v1)을 쓰면 v2 재색인 후 alias 이동으로 무중단 전환하는 전략이 깨진다
        _, kwargs = _search()

        assert kwargs["index"] == INDEX_ALIAS
        assert INDEX_ALIAS == "tech-news-index"

    def test_제목에_가중치를_주고_요약과_함께_검색한다(self):
        _, kwargs = _search()

        fields = kwargs["body"]["query"]["multi_match"]["fields"]
        # 본문 대신 summary — 색인에 원문 본문이 없고 사용자가 읽는 텍스트가 LLM 요약이다
        assert fields == ["title^2", "title.standard^2", "summary", "summary.standard"]
        assert kwargs["body"]["query"]["multi_match"]["query"] == "마이그레이션"

    def test_operator는_AND다(self):
        # 기본값(OR)이면 nori가 쪼갠 한 글자 토큰에 무관한 문서가 대량 매칭된다
        _, kwargs = _search()

        assert kwargs["body"]["query"]["multi_match"]["operator"] == "and"

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
        _, kwargs = _search(TechSearchSort.RELEVANCE)

        assert kwargs["body"]["sort"] == [
            {"_score": {"order": "desc"}},
            {"publishedAt": {"order": "desc"}},
        ]

    def test_최신순은_발행_시각_먼저_동시각이면_점수순(self):
        # 뉴스의 "최신"은 수집 시각이 아니라 발행 시각이다 — 목록 API 정렬과 같은 기준
        _, kwargs = _search(TechSearchSort.LATEST)

        assert kwargs["body"]["sort"] == [
            {"publishedAt": {"order": "desc"}},
            {"_score": {"order": "desc"}},
        ]

    def test_어느_정렬이든_2차_키가_있다(self):
        # 1차 키 동점 시 순서가 흔들리면 페이징에서 중복·누락이 생긴다
        for sort in TechSearchSort:
            _, kwargs = _search(sort)
            assert len(kwargs["body"]["sort"]) == 2


class TestResultMapping:

    def test_응답의_source를_문서로_변환한다(self):
        hits = {
            "hits": {
                "total": {"value": 28},
                "hits": [
                    {
                        "_source": {
                            "id": 900,
                            "title": "Amazon Nova Multimodal Embeddings",
                            "summary": "**한 줄 요약** AWS GovCloud 지원",
                            "link": "https://aws.amazon.com/whats-new/",
                            "sourceName": "AWS What's New",
                            "categoryIds": [2, 3, 5],
                            "publishedAt": "2026-08-12T23:34:00",
                            "createdAt": "2026-08-14T11:10:00",
                        }
                    }
                ],
            }
        }
        result, _ = _search(hits=hits)

        assert result.total_count == 28
        doc = result.documents[0]
        # worker가 색인한 camelCase JSON을 snake_case 모델로 매핑한다
        assert doc.id == 900
        assert doc.source_name == "AWS What's New"
        assert doc.category_ids == [2, 3, 5]
        assert doc.published_at.isoformat() == "2026-08-12T23:34:00"

    def test_결과가_없으면_빈_목록과_0건(self):
        result, _ = _search()

        assert result.documents == []
        assert result.total_count == 0

    def test_total이_없어도_0으로_처리한다(self):
        result, _ = _search(hits={"hits": {"hits": []}})

        assert result.total_count == 0
