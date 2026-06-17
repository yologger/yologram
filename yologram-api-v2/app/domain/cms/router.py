from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.cms.service import CategoryService

router = APIRouter(prefix="/api/v2/cms", tags=["Category"])


@router.get(
    "/{section}/categories",
    response_model=ApiEnvelop,
    summary="카테고리 목록 조회",
    description="섹션(section)별 활성 카테고리를 정렬 순으로 조회",
    responses={
        400: {"description": "유효하지 않은 섹션"},
    },
)
def get_categories(section: str, db: Session = Depends(get_db)):
    service = CategoryService(db)
    result = service.get_categories(section)
    return ApiEnvelop(data=result)
