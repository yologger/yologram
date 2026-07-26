from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.ums.admin_auth_dependency import get_authenticated_admin
from app.domain.ums.admin_schema import AdminAuthData, AdminLoginRequest, AdminUserCreateRequest
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


@router.post(
    "/auth/login",
    response_model=ApiEnvelop,
    summary="어드민 로그인 (어드민 JWT 발급)",
    responses={
        200: {"description": "로그인 성공"},
        400: {"description": "입력값 검증 실패"},
        401: {"description": "비밀번호 불일치"},
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
