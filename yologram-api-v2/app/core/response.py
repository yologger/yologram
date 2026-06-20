from typing import Any, Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class ApiEnvelop(BaseModel):
    data: Any


class ApiEnvelopCursorPage(BaseModel, Generic[T]):
    data: list[T]
    next_cursor: str | None = Field(default=None, serialization_alias="nextCursor")
