from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.domain.tech.news.model import TechNews


class TechNewsResponse(BaseModel):
    """테크 뉴스 (RSS 수집 + LLM 요약)"""

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
    def from_news(cls, news: TechNews, categories: list[str]) -> "TechNewsResponse":
        """SUMMARIZED만 노출하므로 summary는 항상 존재 — 방어적으로 빈 문자열 폴백"""
        return cls(
            id=news.id,
            title=news.title,
            summary=news.summary or "",
            link=news.link,
            source_name=news.source_name,
            categories=categories,
            published_at=news.published_at,
        )
