from typing import Any, Generic, TypeVar

from pydantic import BaseModel, Field, model_serializer

T = TypeVar("T")


class ApiEnvelop(BaseModel):
    data: Any


class ApiEnvelopCursorPage(BaseModel, Generic[T]):
    data: list[T]
    next_cursor: str | None = Field(default=None, serialization_alias="nextCursor")

    # api-v1(@JsonInclude NON_NULL)과 동일하게 커서가 없으면 필드 자체를 생략 (envelope 레벨만 — 항목 내부 null은 유지)
    @model_serializer(mode="wrap")
    def _drop_null_cursor(self, handler):
        result = handler(self)
        for key in ("nextCursor", "next_cursor"):
            if key in result and result[key] is None:
                del result[key]
        return result


class ApiEnvelopPage(BaseModel, Generic[T]):
    """offset 페이지네이션 응답 (page/size/총 개수/총 페이지). cursor와 대비."""

    data: list[T]
    page: int
    size: int
    total_pages: int = Field(serialization_alias="totalPages")
    total_count: int = Field(serialization_alias="totalCount")
    first: bool
    last: bool
