from fastapi import APIRouter, Depends, Query, Response, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop, ApiEnvelopPage
from app.domain.ums.admin_auth_dependency import get_authenticated_admin
from app.domain.ums.admin_schema import (
    AdminAuthData,
    AdminLoginRequest,
    AdminUserCreateRequest,
    AdminUserResponse,
    AdminUserStatusUpdateRequest,
)
from app.domain.ums.admin_service import AdminUserService

router = APIRouter(prefix="/api/v2/ums/admin", tags=["AdminUser"])


@router.post(
    "/admin-users",
    response_model=ApiEnvelop,
    status_code=status.HTTP_201_CREATED,
    summary="어드민 유저 생성",
    description="기존 어드민이 새 어드민 계정을 추가 (어드민 토큰 필요)",
    responses={
        201: {"description": "생성 성공"},
        400: {"description": "입력값 검증 실패"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
        409: {"description": "이미 등록된 어드민 이메일"},
    },
)
def create(
    request: AdminUserCreateRequest,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminUserService(db)
    result = service.create(request)
    return ApiEnvelop(data=result)


@router.get(
    "/admin-users",
    response_model=ApiEnvelopPage[AdminUserResponse],
    summary="어드민 계정 목록 조회",
    description="어드민 계정을 id 오름차순, offset 페이지네이션으로 조회 (어드민 토큰 필요)",
    responses={
        200: {"description": "조회 성공"},
        400: {"description": "잘못된 페이지 파라미터 (VALIDATION_ERROR — page 음수, size 1~100 밖)"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
    },
)
def get_admin_users(
    page: int = Query(default=0, ge=0, description="페이지 번호 (0-based, 기본 0)"),
    size: int = Query(default=10, ge=1, le=100, description="페이지 크기 (기본 10, 1~100)"),
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminUserService(db)
    return service.get_admin_users(page, size)


@router.patch(
    "/admin-users/{id}/status",
    response_model=ApiEnvelop,
    summary="어드민 계정 활성/비활성 변경",
    description="OWNER 전용. status는 ACTIVE/INACTIVE만 허용, OWNER 계정은 변경 불가 (어드민 토큰 필요)",
    responses={
        200: {"description": "변경 성공"},
        400: {
            "description": "비허용 status 값 (VALIDATION_ERROR) 또는 OWNER 대상 변경 시도 (ADMIN_USER_OWNER_IMMUTABLE)"
        },
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
        403: {"description": "요청자가 OWNER가 아님 (ADMIN_ROLE_FORBIDDEN)"},
        404: {"description": "어드민 사용자를 찾을 수 없음 (ADMIN_USER_NOT_FOUND)"},
    },
)
def update_status(
    id: int,
    request: AdminUserStatusUpdateRequest,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminUserService(db)
    return ApiEnvelop(data=service.update_status(auth_data, id, request))


@router.delete(
    "/admin-users/{id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="어드민 계정 삭제",
    description="hard delete — 자기 자신·OWNER 계정은 삭제 불가 (어드민 토큰 필요)",
    responses={
        204: {"description": "삭제 성공"},
        400: {
            "description": "자기 자신 삭제 시도 (ADMIN_USER_SELF_DELETE) 또는 OWNER 삭제 시도 (ADMIN_USER_OWNER_UNDELETABLE)"
        },
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
        404: {"description": "어드민 사용자를 찾을 수 없음 (ADMIN_USER_NOT_FOUND)"},
    },
)
def delete(
    id: int,
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminUserService(db)
    service.delete(auth_data, id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post(
    "/auth/login",
    response_model=ApiEnvelop,
    summary="어드민 로그인 (어드민 JWT 발급)",
    responses={
        200: {"description": "로그인 성공"},
        400: {"description": "입력값 검증 실패"},
        401: {"description": "비밀번호 불일치"},
        403: {"description": "비활성화된 계정 (ADMIN_USER_INACTIVE)"},
        404: {"description": "어드민 사용자를 찾을 수 없음"},
    },
)
def login(request: AdminLoginRequest, db: Session = Depends(get_db)):
    service = AdminUserService(db)
    result = service.login(request)
    return ApiEnvelop(data=result)


@router.post(
    "/auth/validate-token",
    response_model=ApiEnvelop,
    summary="어드민 토큰 검증",
    responses={
        200: {"description": "검증 성공"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
        403: {"description": "비활성화된 계정 (ADMIN_USER_INACTIVE)"},
        404: {"description": "어드민 사용자를 찾을 수 없음"},
    },
)
def validate_token(
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminUserService(db)
    result = service.validate_token(auth_data)
    return ApiEnvelop(data=result)


@router.post(
    "/auth/logout",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="어드민 로그아웃",
    responses={
        204: {"description": "로그아웃 성공"},
        401: {"description": "인증 실패 (어드민 토큰 없음/만료/유효하지 않음)"},
    },
)
def logout(
    auth_data: AdminAuthData = Depends(get_authenticated_admin),
    db: Session = Depends(get_db),
):
    service = AdminUserService(db)
    service.logout(auth_data)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
