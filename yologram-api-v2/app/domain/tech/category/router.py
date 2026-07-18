from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.tech.category.service import TechPostCategoryService

# 기존 /api/v2/cms/{section}/categories의 section 경로변수를 tech로 고정 (URL 결과 동일)
router = APIRouter(prefix="/api/v2/cms", tags=["TechPostCategory"])


@router.get(
    "/tech/categories",
    response_model=ApiEnvelop,
    summary="테크 카테고리 목록 조회",
    description="테크 게시판의 활성 카테고리를 정렬 순으로 조회",
    responses={
        200: {"description": "조회 성공"},
    },
)
def get_post_categories(db: Session = Depends(get_db)):
    service = TechPostCategoryService(db)
    result = service.get_post_categories()
    return ApiEnvelop(data=result)
