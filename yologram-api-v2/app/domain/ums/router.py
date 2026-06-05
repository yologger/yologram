from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.core.response import ApiEnvelop
from app.domain.ums.schema import JoinRequest
from app.domain.ums.service import UserService

router = APIRouter(prefix="/api/v2/ums/user")


@router.post("/join", response_model=ApiEnvelop, status_code=status.HTTP_201_CREATED)
def join(request: JoinRequest, db: Session = Depends(get_db)):
    service = UserService(db)
    result = service.join(request)
    return ApiEnvelop(data=result)
