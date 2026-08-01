from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelopCursorPage
from app.domain.news.tech.schema import TechNewsResponse
from app.domain.news.tech.service import TechNewsService

# 테크 뉴스 공개 조회 API — worker가 수집·요약한 tech_news를 발행순으로 제공.
# 섹션이 경로 세그먼트(/news/tech) — invest/politics 오픈 시 세그먼트 추가 (섹션 규약).
router = APIRouter(prefix="/api/v2/news", tags=["TechNews"])


@router.get(
    "/tech",
    response_model=ApiEnvelopCursorPage[TechNewsResponse],
    summary="테크 뉴스 목록 조회",
    description="요약 완료된 테크 뉴스를 발행순(published_at desc)으로 조회. (publishedAt, id) 복합 keyset cursor",
    responses={
        200: {"description": "조회 성공"},
        400: {"description": "유효하지 않은 커서 (INVALID_CURSOR)"},
    },
)
def get_news(
    category_id: int | None = Query(
        default=None,
        alias="categoryId",
        description="카테고리 id 필터 (tech_category — /cms/tech/categories 응답의 id. 생략 시 전체)",
    ),
    cursor: str | None = Query(default=None, description="이전 페이지 마지막 항목의 커서 (첫 페이지는 생략)"),
    size: int = Query(default=20, description="페이지 크기 (기본 20, 최대 50)"),
    db: Session = Depends(get_db),
):
    service = TechNewsService(db)
    return service.get_news_by_cursor(category_id, cursor, size)
