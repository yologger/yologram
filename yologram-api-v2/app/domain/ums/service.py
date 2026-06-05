import bcrypt
from sqlalchemy.orm import Session

from app.core.exception import UserDuplicateException
from app.domain.ums.model import User
from app.domain.ums.repository import UserRepository
from app.domain.ums.schema import JoinRequest, JoinResponse


class UserService:

    def __init__(self, db: Session):
        self.repository = UserRepository(db)

    def join(self, request: JoinRequest) -> JoinResponse:
        existing = self.repository.find_by_email(request.email)
        if existing:
            raise UserDuplicateException()

        hashed_password = bcrypt.hashpw(
            request.password.encode("utf-8"), bcrypt.gensalt()
        ).decode("utf-8")

        user = User(
            email=request.email,
            name=request.name,
            nickname=request.nickname,
            password=hashed_password,
        )
        saved = self.repository.save(user)
        return JoinResponse(uid=saved.id)
