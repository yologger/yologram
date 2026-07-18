from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.domain.tech.article.model import TechArticle


class TechArticleResponse(BaseModel):
    """테크 아티클 (RSS 수집 + LLM 요약)"""

    model_config = ConfigDict(populate_by_name=True)

    id: int
    title: str
    summary: str = Field(description="LLM 한국어 요약 (마크다운 형식)")
    link: str = Field(description="원문 링크")
    source_name: str = Field(serialization_alias="sourceName", description="출처 (소스명)")
    categories: list[str] = Field(
        description="카테고리 라벨 1~3개 (LLM 분류 — tech_category 마스터 기준)"
    )
    published_at: datetime = Field(serialization_alias="publishedAt")

    @classmethod
    def from_article(cls, article: TechArticle, categories: list[str]) -> "TechArticleResponse":
        """SUMMARIZED만 노출하므로 summary는 항상 존재 — 방어적으로 빈 문자열 폴백"""
        return cls(
            id=article.id,
            title=article.title,
            summary=article.summary or "",
            link=article.link,
            source_name=article.source_name,
            categories=categories,
            published_at=article.published_at,
        )
