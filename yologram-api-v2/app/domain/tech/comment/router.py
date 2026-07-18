from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop, ApiEnvelopCursorPage, ApiEnvelopPage
from app.domain.tech.comment.schema import CommentResponse, CreateCommentRequest, UpdateCommentRequest
from app.domain.tech.comment.service import TechPostCommentService
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData

router = APIRouter(prefix="/api/v2/comments", tags=["TechPostComment"])


# --- 신규 경로 (/comments/tech/...) ---
# tech 고정 경로를 먼저 등록해 구경로({comment_id})와의 매칭 순서를 보장한다.


@router.post(
    "/tech/posts/{post_id}",
    response_model=ApiEnvelop,
    status_code=status.HTTP_201_CREATED,
    summary="테크 댓글 작성",
    description="테크 게시글(postId)에 댓글을 작성 (인증 필요)",
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
    service = TechPostCommentService(db)
    result = service.create(post_id, auth_data.uid, request)
    return ApiEnvelop(data=result)


@router.patch(
    "/tech/{comment_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="테크 댓글 수정",
    description="본인 댓글 수정 (인증 필요)",
    responses={
        204: {"description": "수정 성공"},
        400: {"description": "입력값 검증 실패"},
        401: {"description": "인증 실패"},
        403: {"description": "본인 댓글이 아님"},
        404: {"description": "댓글을 찾을 수 없음"},
    },
)
def update_comment(
    comment_id: int,
    request: UpdateCommentRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostCommentService(db)
    service.update(comment_id, auth_data.uid, request)


@router.delete(
    "/tech/{comment_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="테크 댓글 삭제",
    description="본인 댓글 삭제 (인증 필요)",
    responses={
        204: {"description": "삭제 성공"},
        401: {"description": "인증 실패"},
        403: {"description": "본인 댓글이 아님"},
        404: {"description": "댓글을 찾을 수 없음"},
    },
)
def delete_comment(
    comment_id: int,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostCommentService(db)
    service.delete(comment_id, auth_data.uid)


# cursor-based pagination
@router.get(
    "/tech/posts/{post_id}",
    response_model=ApiEnvelopCursorPage[CommentResponse],
    summary="테크 댓글 목록 조회",
    description="테크 게시글(postId)의 댓글. 최신순(기본)/오래된순 cursor 페이지네이션 (공개)",
    responses={
        200: {"description": "조회 성공"},
        400: {"description": "유효하지 않은 커서"},
    },
)
def get_comments(
    post_id: int,
    sort: str | None = Query(default=None),
    cursor: str | None = Query(default=None),
    size: int = Query(default=20),
    db: Session = Depends(get_db),
):
    service = TechPostCommentService(db)
    return service.get_comments_by_cursor(post_id, sort, cursor, size)


# 댓글 목록 offset 페이지네이션 — 학습용. cursor 방식(/tech/posts/{post_id})과 대비.
# 코드는 TechPostCommentService.get_comments_by_offset에 보존, 엔드포인트는 비활성(필요 시 주석 해제)
# @router.get(
#     "/tech/posts/{post_id}/offset",
#     response_model=ApiEnvelopPage[CommentResponse],
#     summary="테크 댓글 목록 조회 (offset, 학습용)",
#     description="테크 게시글(postId)의 댓글. offset 페이지네이션 + 전체 count (공개). cursor 방식과 대비되는 학습용",
#     responses={
#         200: {"description": "조회 성공"},
#     },
# )
# def get_comments_by_offset(
#     post_id: int,
#     sort: str | None = Query(default=None),
#     page: int = Query(default=0),
#     size: int = Query(default=20),
#     db: Session = Depends(get_db),
# ):
#     service = TechPostCommentService(db)
#     return service.get_comments_by_offset(post_id, sort, page, size)


# --- 구경로 호환 (deprecated) ---
# web이 신규 경로(/comments/tech/...)로 전환할 때까지 유지. tech 서비스로 위임.


@router.post(
    "/posts/{post_id}",
    response_model=ApiEnvelop,
    status_code=status.HTTP_201_CREATED,
    deprecated=True,
    summary="댓글 작성 (구경로)",
    description="구경로 호환용 — /api/v2/comments/tech/posts/{postId} 사용 권장. 테크 댓글 작성으로 위임 (인증 필요)",
    responses={
        201: {"description": "작성 성공"},
        400: {"description": "입력값 검증 실패"},
        401: {"description": "인증 실패"},
        404: {"description": "대상 게시글을 찾을 수 없음"},
    },
)
def create_comment_legacy(
    post_id: int,
    request: CreateCommentRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostCommentService(db)
    result = service.create(post_id, auth_data.uid, request)
    return ApiEnvelop(data=result)


@router.patch(
    "/{comment_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    deprecated=True,
    summary="댓글 수정 (구경로)",
    description="구경로 호환용 — /api/v2/comments/tech/{commentId} 사용 권장. 테크 댓글 수정으로 위임 (인증 필요)",
    responses={
        204: {"description": "수정 성공"},
        400: {"description": "입력값 검증 실패"},
        401: {"description": "인증 실패"},
        403: {"description": "본인 댓글이 아님"},
        404: {"description": "댓글을 찾을 수 없음"},
    },
)
def update_comment_legacy(
    comment_id: int,
    request: UpdateCommentRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostCommentService(db)
    service.update(comment_id, auth_data.uid, request)


@router.delete(
    "/{comment_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    deprecated=True,
    summary="댓글 삭제 (구경로)",
    description="구경로 호환용 — /api/v2/comments/tech/{commentId} 사용 권장. 테크 댓글 삭제로 위임 (인증 필요)",
    responses={
        204: {"description": "삭제 성공"},
        401: {"description": "인증 실패"},
        403: {"description": "본인 댓글이 아님"},
        404: {"description": "댓글을 찾을 수 없음"},
    },
)
def delete_comment_legacy(
    comment_id: int,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostCommentService(db)
    service.delete(comment_id, auth_data.uid)


@router.get(
    "/posts/{post_id}",
    response_model=ApiEnvelopCursorPage[CommentResponse],
    deprecated=True,
    summary="댓글 목록 조회 (구경로)",
    description="구경로 호환용 — /api/v2/comments/tech/posts/{postId} 사용 권장. 테크 댓글 목록 조회로 위임 (공개)",
    responses={
        200: {"description": "조회 성공"},
        400: {"description": "유효하지 않은 커서"},
    },
)
def get_comments_legacy(
    post_id: int,
    sort: str | None = Query(default=None),
    cursor: str | None = Query(default=None),
    size: int = Query(default=20),
    db: Session = Depends(get_db),
):
    service = TechPostCommentService(db)
    return service.get_comments_by_cursor(post_id, sort, cursor, size)
