import bcrypt
from sqlalchemy.orm import Session

from app.core.exception import UserDuplicateException, UserNotFoundException
from app.domain.ums.model import User
from app.domain.ums.repository import UserRepository
from app.domain.ums.schema import JoinRequest, JoinResponse, UserMeResponse


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

    def get_me(self, uid: int) -> UserMeResponse:
        user = self.repository.find_by_id(uid)
        if not user:
            raise UserNotFoundException()

        return UserMeResponse(
            uid=user.id,
            email=user.email,
            name=user.name,
            nickname=user.nickname,
            avatar=user.avatar,
            type=user.type.value,
            joined_date=user.joined_date,
        )
