from typing import Any, Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class ApiEnvelop(BaseModel):
    data: Any


class ApiEnvelopCursorPage(BaseModel, Generic[T]):
    data: list[T]
    next_cursor: str | None = Field(default=None, serialization_alias="nextCursor")


class ApiEnvelopPage(BaseModel, Generic[T]):
    """offset 페이지네이션 응답 (page/size/총 개수/총 페이지). cursor와 대비."""

    data: list[T]
    page: int
    size: int
    total_pages: int = Field(serialization_alias="totalPages")
    total_count: int = Field(serialization_alias="totalCount")
    first: bool
    last: bool
