from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData
from app.domain.ums.schema import ChangePasswordRequest, JoinRequest, UpdateProfileRequest
from app.domain.ums.service import UserService

router = APIRouter(prefix="/api/v2/ums/user", tags=["User"])


@router.post("/join", response_model=ApiEnvelop, status_code=status.HTTP_201_CREATED, summary="회원가입")
def join(request: JoinRequest, db: Session = Depends(get_db)):
    service = UserService(db)
    result = service.join(request)
    return ApiEnvelop(data=result)


@router.get(
    "/me",
    response_model=ApiEnvelop,
    summary="내 정보 조회",
    description="인증된 사용자의 프로필 정보를 조회",
    responses={
        401: {"description": "인증 실패 (토큰 없음/만료/유효하지 않음)"},
        404: {"description": "사용자를 찾을 수 없음"},
    },
)
def get_me(
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = UserService(db)
    result = service.get_me(auth_data.uid)
    return ApiEnvelop(data=result)


@router.patch(
    "/me",
    response_model=ApiEnvelop,
    summary="회원정보 수정",
    description="닉네임 변경",
    responses={
        400: {"description": "입력값 검증 실패"},
        401: {"description": "인증 실패"},
        404: {"description": "사용자를 찾을 수 없음"},
    },
)
def update_profile(
    request: UpdateProfileRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = UserService(db)
    result = service.update_profile(auth_data.uid, request)
    return ApiEnvelop(data=result)


@router.patch(
    "/me/password",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="비밀번호 변경",
    description="현재 비밀번호 확인 후 새 비밀번호로 변경",
    responses={
        400: {"description": "입력값 검증 실패"},
        401: {"description": "인증 실패 또는 현재 비밀번호 불일치"},
        404: {"description": "사용자를 찾을 수 없음"},
    },
)
def change_password(
    request: ChangePasswordRequest,
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = UserService(db)
    service.change_password(auth_data.uid, request)
