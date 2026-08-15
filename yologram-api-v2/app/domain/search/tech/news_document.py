from datetime import datetime

from pydantic import BaseModel, ConfigDict


class TechNewsDocument(BaseModel):
    """
    색인된 뉴스 문서 (읽기 전용) — worker TechNewsDocument·api-v1과 같은 스키마다.
    세 프로젝트가 문자열 계약으로 맞물리므로 필드를 바꾸면 함께 고쳐야 한다.

    JSON은 camelCase(worker가 Jackson으로 색인)이고 파이썬은 snake_case라 from_source에서 매핑한다.
    카테고리는 id만 색인한다 — 라벨은 tech_category 마스터에서 바뀔 수 있어
    색인에 넣으면 이름이 바뀔 때마다 재색인이 필요하다(응답 조립 시 cms에서 해석).
    """

    model_config = ConfigDict(populate_by_name=True)

    id: int
    title: str = ""
    summary: str = ""
    link: str = ""
    source_name: str = ""
    category_ids: list[int] = []
    published_at: datetime | None = None
    created_at: datetime | None = None

    @classmethod
    def from_source(cls, source: dict) -> "TechNewsDocument":
        """OpenSearch _source(camelCase) → 문서 모델"""
        return cls(
            id=source["id"],
            title=source.get("title", ""),
            summary=source.get("summary", ""),
            link=source.get("link", ""),
            source_name=source.get("sourceName", ""),
            category_ids=source.get("categoryIds") or [],
            published_at=source.get("publishedAt"),
            created_at=source.get("createdAt"),
        )
