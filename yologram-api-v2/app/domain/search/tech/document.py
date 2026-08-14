from datetime import datetime

from pydantic import BaseModel, ConfigDict


class TechPostDocumentMetrics(BaseModel):
    """색인된 카운트 — 개인화 값(likedByMe)은 문서에 없다"""

    comment_count: int = 0
    like_count: int = 0
    view_count: int = 0


class TechPostDocument(BaseModel):
    """
    색인된 게시글 문서 (읽기 전용) — worker TechPostDocument·api-v1과 같은 스키마다.
    세 프로젝트가 문자열 계약으로 맞물리므로 필드를 바꾸면 함께 고쳐야 한다.

    JSON은 camelCase(worker가 Jackson으로 색인)이고 파이썬은 snake_case라 alias로 매핑한다.
    작성자 닉네임은 색인에 없다(uid만) — 닉네임이 바뀔 때 재색인이 필요해지므로 응답 조립 시 조회한다.
    """

    model_config = ConfigDict(populate_by_name=True)

    id: int
    uid: int
    title: str | None = None
    content: str = ""
    category_ids: list[int] = []
    metrics: TechPostDocumentMetrics = TechPostDocumentMetrics()
    created_at: datetime | None = None
    modified_at: datetime | None = None

    @classmethod
    def from_source(cls, source: dict) -> "TechPostDocument":
        """OpenSearch _source(camelCase) → 문서 모델"""
        metrics = source.get("metrics") or {}
        return cls(
            id=source["id"],
            uid=source["uid"],
            title=source.get("title"),
            content=source.get("content", ""),
            category_ids=source.get("categoryIds") or [],
            metrics=TechPostDocumentMetrics(
                comment_count=metrics.get("commentCount", 0),
                like_count=metrics.get("likeCount", 0),
                view_count=metrics.get("viewCount", 0),
            ),
            created_at=source.get("createdAt"),
            modified_at=source.get("modifiedAt"),
        )
