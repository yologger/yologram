from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.domain.cms.enum import Section


class CreatePostRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    title: str | None = Field(default=None, max_length=200)
    content: str | None = Field(default=None, validate_default=True)
    category_ids: list[int] = Field(default_factory=list, validate_default=True, alias="categoryIds")

    @field_validator("content")
    @classmethod
    def content_not_blank(cls, v: str | None) -> str:
        if v is None or not v.strip():
            raise ValueError("내용을 입력해주세요.")
        return v

    @field_validator("category_ids")
    @classmethod
    def category_ids_count(cls, v: list[int]) -> list[int]:
        if not 1 <= len(v) <= 3:
            raise ValueError("카테고리는 1~3개 선택해주세요.")
        return v


class CreatePostResponse(BaseModel):
    id: int


class PostAuthor(BaseModel):
    uid: int
    nickname: str | None


class PostDetailResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    id: int
    section: Section
    author: PostAuthor
    title: str | None
    content: str
    category_ids: list[int] = Field(serialization_alias="categoryIds")
    like_count: int = Field(serialization_alias="likeCount")
    comment_count: int = Field(serialization_alias="commentCount")
    created_at: datetime = Field(serialization_alias="createdAt")


class PostSummaryResponse(BaseModel):
    """목록 항목. 피드에서 본문 노출을 위해 content 전체 포함 (상세와 동일 필드)."""

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    id: int
    section: Section
    author: PostAuthor
    title: str | None
    content: str
    category_ids: list[int] = Field(serialization_alias="categoryIds")
    like_count: int = Field(serialization_alias="likeCount")
    comment_count: int = Field(serialization_alias="commentCount")
    created_at: datetime = Field(serialization_alias="createdAt")
