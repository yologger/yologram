from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator


class CreateCommentRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    content: str | None = Field(default=None, max_length=1000, validate_default=True)

    @field_validator("content")
    @classmethod
    def content_not_blank(cls, v: str | None) -> str:
        if v is None or not v.strip():
            raise ValueError("내용을 입력해주세요.")
        return v


class CreateCommentResponse(BaseModel):
    id: int


class CommentAuthor(BaseModel):
    uid: int
    nickname: str | None


class CommentResponse(BaseModel):
    """댓글 목록 항목. api-v1 CommentResponse와 동일 직렬화(postId, createdAt는 camelCase)."""

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    id: int
    post_id: int = Field(serialization_alias="postId")
    author: CommentAuthor
    content: str
    created_at: datetime = Field(serialization_alias="createdAt")
