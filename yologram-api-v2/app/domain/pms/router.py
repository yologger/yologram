from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.pms.schema import CreatePostRequest
from app.domain.pms.service import PostService
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData

router = APIRouter(prefix="/api/v2/pms", tags=["Post"])


@router.post(
    "/{section}/posts",
    response_model=ApiEnvelop,
    status_code=status.HTTP_201_CREATED,
    summary="게시글 작성",
    description="섹션(section)에 게시글을 작성 (인증 필요)",
    responses={
        201: {"description": "작성 성공"},
        400: {"description": "입력값 검증 실패 / 유효하지 않은 섹션 / 카테고리 불일치"},
        401: {"description": "인증 실패"},
    },
)
def create_post(
    section: str,
    request: CreatePostRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = PostService(db)
    result = service.create(section, auth_data.uid, request)
    return ApiEnvelop(data=result)
