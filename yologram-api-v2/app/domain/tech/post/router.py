from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop, ApiEnvelopCursorPage, ApiEnvelopPage
from app.domain.tech.post.schema import CreatePostRequest, PostSummaryResponse, UpdatePostRequest
from app.domain.tech.post.service import TechPostService
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData

# 기존 /api/v2/pms/{section}/posts의 section 경로변수를 tech로 고정 (URL 결과 동일)
router = APIRouter(prefix="/api/v2/pms", tags=["TechPost"])


@router.post(
    "/tech/posts",
    response_model=ApiEnvelop,
    status_code=status.HTTP_201_CREATED,
    summary="테크 게시글 작성",
    description="테크 게시판에 게시글을 작성 (인증 필요)",
    responses={
        201: {"description": "작성 성공"},
        400: {"description": "입력값 검증 실패 / 카테고리 불일치"},
        401: {"description": "인증 실패"},
    },
)
def create_post(
    request: CreatePostRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostService(db)
    result = service.create(auth_data.uid, request)
    return ApiEnvelop(data=result)


@router.patch(
    "/tech/posts/{id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="테크 게시글 수정",
    description="본인 게시글 수정 (인증 필요)",
    responses={
        204: {"description": "수정 성공"},
        400: {"description": "입력값 검증 실패 / 카테고리 불일치"},
        401: {"description": "인증 실패"},
        403: {"description": "본인 글이 아님"},
        404: {"description": "게시글을 찾을 수 없음"},
    },
)
def update_post(
    id: int,
    request: UpdatePostRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostService(db)
    service.update(id, auth_data.uid, request)


@router.delete(
    "/tech/posts/{id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="테크 게시글 삭제",
    description="본인 게시글 삭제 (인증 필요)",
    responses={
        204: {"description": "삭제 성공"},
        401: {"description": "인증 실패"},
        403: {"description": "본인 글이 아님"},
        404: {"description": "게시글을 찾을 수 없음"},
    },
)
def delete_post(
    id: int,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostService(db)
    service.delete(id, auth_data.uid)


# cursor-based pagination
@router.get(
    "/tech/posts",
    response_model=ApiEnvelopCursorPage[PostSummaryResponse],
    summary="테크 게시글 목록 조회",
    description="테크 피드. 최신순(id desc) cursor 페이지네이션 (공개)",
    responses={
        200: {"description": "조회 성공"},
        400: {"description": "유효하지 않은 커서"},
    },
)
def get_posts(
    cursor: str | None = Query(default=None),
    size: int = Query(default=20),
    category_id: int | None = Query(default=None, alias="categoryId"),
    db: Session = Depends(get_db),
):
    service = TechPostService(db)
    return service.get_posts_by_cursor(category_id, cursor, size)


# 테크 피드 offset 페이지네이션 — 학습용. cursor 방식(/tech/posts)과 대비.
# 코드는 TechPostService.get_posts_by_offset에 보존, 엔드포인트는 비활성(필요 시 주석 해제)
# @router.get(
#     "/tech/posts/offset",
#     response_model=ApiEnvelopPage[PostSummaryResponse],
#     summary="테크 게시글 목록 조회 (offset, 학습용)",
#     description="테크 피드. offset 페이지네이션 + 전체 count (공개). cursor 방식과 대비되는 학습용",
#     responses={
#         200: {"description": "조회 성공"},
#     },
# )
# def get_posts_by_offset(
#     category_id: int | None = Query(default=None, alias="categoryId"),
#     page: int = Query(default=0),
#     size: int = Query(default=20),
#     db: Session = Depends(get_db),
# ):
#     service = TechPostService(db)
#     return service.get_posts_by_offset(category_id, page, size)


# 내 글 목록 (cursor) — 실사용
@router.get(
    "/posts/me",
    response_model=ApiEnvelopCursorPage[PostSummaryResponse],
    summary="내 글 목록 조회",
    description="로그인 유저가 작성한 글. 최신순 cursor 페이지네이션 (인증 필요). section은 생략 또는 tech만 허용",
    responses={
        200: {"description": "조회 성공"},
        400: {"description": "유효하지 않은 섹션 / 커서"},
        401: {"description": "인증 실패"},
    },
)
def get_my_posts_by_cursor(
    section: str | None = Query(default=None),
    cursor: str | None = Query(default=None),
    size: int = Query(default=20),
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = TechPostService(db)
    return service.get_my_posts_by_cursor(auth_data.uid, section, cursor, size)


# 내 글 목록 (offset) — 학습용. cursor 방식(/posts/me)과 대비.
# 코드는 TechPostService.get_my_posts_by_offset에 보존, 엔드포인트는 비활성(필요 시 주석 해제)
# @router.get(
#     "/posts/me/offset",
#     response_model=ApiEnvelopPage[PostSummaryResponse],
#     summary="내 글 목록 조회 (offset, 학습용)",
#     description="로그인 유저가 작성한 글. offset 페이지네이션 + 전체 count (인증 필요). cursor 방식과 대비되는 학습용",
#     responses={
#         200: {"description": "조회 성공"},
#         400: {"description": "유효하지 않은 섹션"},
#         401: {"description": "인증 실패"},
#     },
# )
# def get_my_posts_by_offset(
#     section: str | None = Query(default=None),
#     page: int = Query(default=0),
#     size: int = Query(default=20),
#     auth_data: AuthData = Depends(get_authenticated_user),
#     db: Session = Depends(get_db),
# ):
#     service = TechPostService(db)
#     return service.get_my_posts_by_offset(auth_data.uid, section, page, size)


@router.get(
    "/tech/posts/{id}",
    response_model=ApiEnvelop,
    summary="테크 게시글 상세 조회",
    description="테크 게시판의 게시글 단건 조회 (공개)",
    responses={
        200: {"description": "조회 성공"},
        404: {"description": "게시글을 찾을 수 없음"},
    },
)
def get_post(
    id: int,
    db: Session = Depends(get_db),
):
    service = TechPostService(db)
    result = service.get_post(id)
    return ApiEnvelop(data=result)
