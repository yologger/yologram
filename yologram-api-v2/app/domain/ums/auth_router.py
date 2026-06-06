from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.ums.auth_dependency import get_authenticated_user
from app.domain.ums.auth_schema import AuthData, LoginRequest
from app.domain.ums.auth_service import AuthService

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
