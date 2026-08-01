import re
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.domain.news.tech.model import TechNewsSource

# api-v1 @Pattern("^https?://\\S+$") 미러
URL_PATTERN = re.compile(r"^https?://\S+$")


def _validate_name(v: str) -> str:
    """api-v1 NotBlank + Size(1~100) 미러 — 검증 메시지 동일"""
    if not v.strip():
        raise ValueError("소스 이름을 입력해주세요")
    if len(v) > 100:
        raise ValueError("소스 이름은 1~100자여야 합니다")
    return v


def _validate_url(v: str) -> str:
    """api-v1 NotBlank + Size(≤500) + Pattern(http/https) 미러 — 검증 메시지 동일"""
    if not v.strip():
        raise ValueError("RSS 피드 URL을 입력해주세요")
    if len(v) > 500:
        raise ValueError("URL은 500자 이하여야 합니다")
    if not URL_PATTERN.match(v):
        raise ValueError("URL은 http/https 형식이어야 합니다")
    return v


class AdminTechNewsSourceCreateRequest(BaseModel):
    """어드민 테크 뉴스 소스 생성 요청"""

    model_config = ConfigDict(populate_by_name=True)

    name: str = Field(description="소스 이름 (1~100자)", examples=["GeekNews"])
    url: str = Field(
        description="RSS 피드 URL (http/https, 500자 이하)",
        examples=["https://news.hada.io/rss/news"],
    )
    is_active: bool = Field(default=True, alias="isActive", description="수집 활성 여부 (생략 시 true)")

    @field_validator("name")
    @classmethod
    def name_valid(cls, v: str) -> str:
        return _validate_name(v)

    @field_validator("url")
    @classmethod
    def url_valid(cls, v: str) -> str:
        return _validate_url(v)


class AdminTechNewsSourceUpdateRequest(BaseModel):
    """어드민 테크 뉴스 소스 수정 요청 — 널 필드는 미변경 (부분 갱신)"""

    model_config = ConfigDict(populate_by_name=True)

    name: str | None = Field(default=None, description="소스 이름 (미변경 시 생략)", examples=["GeekNews"])
    url: str | None = Field(
        default=None,
        description="RSS 피드 URL (미변경 시 생략)",
        examples=["https://news.hada.io/rss/news"],
    )
    is_active: bool | None = Field(
        default=None, alias="isActive", description="수집 활성 여부 (미변경 시 생략)", examples=[False]
    )

    @field_validator("name")
    @classmethod
    def name_valid(cls, v: str | None) -> str | None:
        return v if v is None else _validate_name(v)

    @field_validator("url")
    @classmethod
    def url_valid(cls, v: str | None) -> str | None:
        return v if v is None else _validate_url(v)


class AdminTechNewsSourceResponse(BaseModel):
    """어드민 테크 뉴스 소스"""

    model_config = ConfigDict(populate_by_name=True)

    id: int
    name: str = Field(description="소스 이름")
    url: str = Field(description="RSS 피드 URL")
    is_active: bool = Field(serialization_alias="isActive", description="수집 활성 여부")
    created_at: datetime = Field(serialization_alias="createdAt")
    modified_date: datetime = Field(serialization_alias="modifiedDate")

    @classmethod
    def from_source(cls, source: TechNewsSource) -> "AdminTechNewsSourceResponse":
        return cls(
            id=source.id,
            name=source.name,
            url=source.url,
            is_active=source.is_active,
            created_at=source.created_at,
            modified_date=source.modified_date,
        )
