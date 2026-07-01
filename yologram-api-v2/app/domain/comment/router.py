from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.comment.schema import CreateCommentRequest
from app.domain.comment.service import CommentService
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData

router = APIRouter(prefix="/api/v2/comments", tags=["Comment"])


@router.post(
    "/posts/{post_id}",
    response_model=ApiEnvelop,
    status_code=status.HTTP_201_CREATED,
    summary="댓글 작성",
    description="게시글(postId)에 댓글을 작성 (인증 필요)",
    responses={
        201: {"description": "작성 성공"},
        400: {"description": "입력값 검증 실패"},
        401: {"description": "인증 실패"},
        404: {"description": "대상 게시글을 찾을 수 없음"},
    },
)
def create_comment(
    post_id: int,
    request: CreateCommentRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = CommentService(db)
    result = service.create(post_id, auth_data.uid, request)
    return ApiEnvelop(data=result)
