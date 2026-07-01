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
