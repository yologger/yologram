from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator


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


class UpdatePostRequest(BaseModel):
    """게시글 수정 요청 (작성과 동일 검증)."""

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


class PostMetrics(BaseModel):
    """게시글 카운트 지표 묶음 — 목록·상세 응답 공용 (레거시 product.metrics 미러, api-v1 TechPostMetrics 정합).
    평면 likeCount/commentCount 필드를 대체. viewCount는 조회수 도입 시 필드 추가(무브레이킹).
    likedByMe는 개인화 값이지만 사용자 결정으로 metrics 안에 포함 — 비로그인이면 False."""

    model_config = ConfigDict(populate_by_name=True)

    comment_count: int = Field(serialization_alias="commentCount")
    like_count: int = Field(serialization_alias="likeCount")
    liked_by_me: bool = Field(serialization_alias="likedByMe")


class PostDetailResponse(BaseModel):
    """상세 응답. 카운트는 metrics 객체로 중첩 (2026-08 계약 전환 — web metrics 참조 전환과 한 트랙).
    section은 테이블 분리 후에도 응답 계약 유지를 위해 상수 "TECH"로 직렬화."""

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    id: int
    section: str = "TECH"
    author: PostAuthor
    title: str | None
    content: str
    category_ids: list[int] = Field(serialization_alias="categoryIds")
    metrics: PostMetrics
    created_at: datetime = Field(serialization_alias="createdAt")


class PostSummaryResponse(BaseModel):
    """목록 항목. 피드에서 본문 노출을 위해 content 전체 포함 (상세와 동일 필드).
    카운트는 metrics 객체로 중첩. section은 응답 계약 유지를 위해 상수 "TECH"로 직렬화."""

    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    id: int
    section: str = "TECH"
    author: PostAuthor
    title: str | None
    content: str
    category_ids: list[int] = Field(serialization_alias="categoryIds")
    metrics: PostMetrics
    created_at: datetime = Field(serialization_alias="createdAt")
