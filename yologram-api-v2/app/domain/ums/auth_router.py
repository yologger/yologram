from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData, EmailVerificationSendRequest, EmailVerificationVerifyRequest, LoginRequest
from app.domain.ums.auth_service import AuthService
from app.domain.ums.email_dependency import get_email_sender
from app.domain.ums.email_sender import EmailSender, StubEmailSender
from app.domain.ums.email_verification_service import EmailVerificationService

router = APIRouter(prefix="/api/v2/ums/auth")


@router.post("/login", response_model=ApiEnvelop)
def login(request: LoginRequest, db: Session = Depends(get_db)):
    service = AuthService(db)
    result = service.login(request)
    return ApiEnvelop(data=result)


@router.post("/validate-token", response_model=ApiEnvelop)
def validate_token(
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = AuthService(db)
    result = service.validate_token(auth_data)
    return ApiEnvelop(data=result)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
def logout(
    auth_data: AuthData = Depends(get_authenticated_user),
    db: Session = Depends(get_db),
):
    service = AuthService(db)
    service.logout(auth_data)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post(
    "/email-verification/send",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="이메일 인증 코드 발송",
    description="회원가입 전 이메일 인증 코드를 발송 (5분 유효)",
    responses={
        204: {"description": "발송 성공"},
        409: {"description": "이미 가입된 이메일"},
        422: {"description": "입력값 검증 실패"},
    },
)
def send_code(
    request: EmailVerificationSendRequest,
    db: Session = Depends(get_db),
    email_sender: EmailSender = Depends(get_email_sender),
):
    service = EmailVerificationService(db, email_sender)
    service.send_code(request.email)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post(
    "/email-verification/verify",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="이메일 인증 코드 검증",
    description="발송된 인증 코드를 검증",
    responses={
        204: {"description": "인증 성공"},
        400: {"description": "인증 코드 불일치 또는 만료"},
        422: {"description": "입력값 검증 실패"},
    },
)
def verify_code(
    request: EmailVerificationVerifyRequest,
    db: Session = Depends(get_db),
):
    service = EmailVerificationService(db, StubEmailSender())
    service.verify_code(request.email, request.code)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
