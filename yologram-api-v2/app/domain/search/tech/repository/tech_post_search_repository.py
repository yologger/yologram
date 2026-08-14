from dataclasses import dataclass

from app.config.opensearch import get_opensearch_client
from app.domain.search.tech.document import TechPostDocument
from app.domain.search.tech.model import TechPostSearchSort

# alias — 실제 인덱스는 tech-post-index-v1 (worker가 생성·관리).
# 실제 인덱스명을 쓰면 v2 재색인 후 alias 이동으로 무중단 전환하는 전략이 깨진다
INDEX_ALIAS = "tech-post-index"

_FIELD_TITLE_BOOSTED = "title^2"
_FIELD_CONTENT = "content"
_FIELD_CREATED_AT = "createdAt"


@dataclass(frozen=True)
class TechPostSearchResult:
    """문서 목록과 전체 매칭 수(페이지 네비게이션의 분모)"""

    documents: list[TechPostDocument]
    total_count: int


class TechPostSearchRepository:
    """
    게시글 검색 질의 (api-v1 TechPostSearchRepository 미러) —
    OpenSearch 접근은 이 층에만 둔다(서비스는 문서·총건수만 받는다).
    """

    def search(
        self, keyword: str, from_: int, size: int, sort: TechPostSearchSort
    ) -> TechPostSearchResult:
        body = {
            "from": from_,
            "size": size,
            # 제목 가중치 2배 — 제목이 맞는 글이 본문만 맞는 글보다 위로 온다.
            # nori 분석기가 적용된 필드라 형태소 단위로 매칭된다
            "query": {
                "multi_match": {
                    "query": keyword,
                    "fields": [_FIELD_TITLE_BOOSTED, _FIELD_CONTENT],
                }
            },
            "sort": self._sort_clause(sort),
            # 총건수는 정확한 값이 필요하다 — 기본값(10000)이면 그 이상에서 카운트가 고정돼
            # 페이지 네비게이션의 마지막 페이지가 틀어진다
            "track_total_hits": True,
        }

        response = get_opensearch_client().search(index=INDEX_ALIAS, body=body)
        hits = response.get("hits", {})

        return TechPostSearchResult(
            documents=[
                TechPostDocument.from_source(hit["_source"])
                for hit in hits.get("hits", [])
                if hit.get("_source")
            ],
            total_count=(hits.get("total") or {}).get("value", 0),
        )

    @staticmethod
    def _sort_clause(sort: TechPostSearchSort) -> list[dict]:
        """2차 키를 두는 이유: 1차 키 동점 시 순서가 흔들리면 페이징에서 중복·누락이 생긴다"""
        if sort is TechPostSearchSort.LATEST:
            return [{_FIELD_CREATED_AT: {"order": "desc"}}, {"_score": {"order": "desc"}}]
        return [{"_score": {"order": "desc"}}, {_FIELD_CREATED_AT: {"order": "desc"}}]
