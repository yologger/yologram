from dataclasses import dataclass

from app.config.opensearch import get_opensearch_client
from app.domain.search.tech.model import TechSearchSort
from app.domain.search.tech.news_document import TechNewsDocument

# alias — 실제 인덱스는 tech-news-index-v1 (worker가 생성·관리).
# 실제 인덱스명을 쓰면 v2 재색인 후 alias 이동으로 무중단 전환하는 전략이 깨진다
INDEX_ALIAS = "tech-news-index"

_FIELD_TITLE_BOOSTED = "title^2"
_FIELD_TITLE_STANDARD = "title.standard^2"
_FIELD_SUMMARY = "summary"
_FIELD_SUMMARY_STANDARD = "summary.standard"
_FIELD_PUBLISHED_AT = "publishedAt"


@dataclass(frozen=True)
class TechNewsSearchResult:
    """문서 목록과 전체 매칭 수(페이지 네비게이션의 분모)"""

    documents: list[TechNewsDocument]
    total_count: int


class TechNewsSearchRepository:
    """
    뉴스 검색 질의 (api-v1 TechNewsSearchRepository 미러) —
    게시글 검색과 같은 구조이고 인덱스·필드만 다르다.

    본문 대신 summary를 검색한다: 색인에 원문 본문이 없다(RSS는 요약·발췌만 주고,
    우리가 가진 전문은 LLM 한국어 요약이다). 사용자가 읽는 텍스트와 검색 대상이 같아진다.
    """

    def search(
        self, keyword: str, from_: int, size: int, sort: TechSearchSort
    ) -> TechNewsSearchResult:
        body = {
            "from": from_,
            "size": size,
            # 제목 가중치 2배 — 제목이 맞는 뉴스가 요약만 맞는 뉴스보다 위로 온다.
            # nori 필드는 형태소 단위로, standard 필드는 단어를 통째로 매칭한다.
            # 둘 다 보는 이유: nori는 사전에 없는 외래어를 문맥마다 다르게 쪼갠다
            "query": {
                "multi_match": {
                    "query": keyword,
                    "fields": [
                        _FIELD_TITLE_BOOSTED,
                        _FIELD_TITLE_STANDARD,
                        _FIELD_SUMMARY,
                        _FIELD_SUMMARY_STANDARD,
                    ],
                    # AND — 기본값(OR)이면 nori가 쪼갠 한 글자 토큰에 무관한 문서가 대량으로 매칭된다
                    "operator": "and",
                }
            },
            "sort": self._sort_clause(sort),
            # 총건수는 정확한 값이 필요하다 — 기본값(10000)이면 그 이상에서 카운트가 고정돼
            # 페이지 네비게이션의 마지막 페이지가 틀어진다
            "track_total_hits": True,
        }

        response = get_opensearch_client().search(index=INDEX_ALIAS, body=body)
        hits = response.get("hits", {})

        return TechNewsSearchResult(
            documents=[
                TechNewsDocument.from_source(hit["_source"])
                for hit in hits.get("hits", [])
                if hit.get("_source")
            ],
            total_count=(hits.get("total") or {}).get("value", 0),
        )

    @staticmethod
    def _sort_clause(sort: TechSearchSort) -> list[dict]:
        """2차 키를 두는 이유: 1차 키 동점 시 순서가 흔들리면 페이징에서 중복·누락이 생긴다"""
        if sort is TechSearchSort.LATEST:
            # 뉴스의 "최신"은 수집 시각이 아니라 발행 시각이다 — 목록 API 정렬과 같은 기준
            return [{_FIELD_PUBLISHED_AT: {"order": "desc"}}, {"_score": {"order": "desc"}}]
        return [{"_score": {"order": "desc"}}, {_FIELD_PUBLISHED_AT: {"order": "desc"}}]
