from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop, ApiEnvelopCursorPage
from app.domain.pms.schema import CreatePostRequest, PostSummaryResponse
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


@router.get(
    "/{section}/posts",
    response_model=ApiEnvelopCursorPage[PostSummaryResponse],
    summary="게시글 목록 조회",
    description="섹션(section) 피드. 최신순(id desc) cursor 페이지네이션 (공개)",
    responses={
        200: {"description": "조회 성공"},
        400: {"description": "유효하지 않은 섹션 / 커서"},
    },
)
def get_posts(
    section: str,
    cursor: str | None = Query(default=None),
    size: int = Query(default=20),
    category_id: int | None = Query(default=None, alias="categoryId"),
    db: Session = Depends(get_db),
):
    service = PostService(db)
    return service.get_posts(section, category_id, cursor, size)


@router.get(
    "/{section}/posts/{id}",
    response_model=ApiEnvelop,
    summary="게시글 상세 조회",
    description="섹션(section)의 게시글 단건 조회 (공개)",
    responses={
        200: {"description": "조회 성공"},
        400: {"description": "유효하지 않은 섹션"},
        404: {"description": "게시글을 찾을 수 없음"},
    },
)
def get_post(
    section: str,
    id: int,
    db: Session = Depends(get_db),
):
    service = PostService(db)
    result = service.get_post(section, id)
    return ApiEnvelop(data=result)
