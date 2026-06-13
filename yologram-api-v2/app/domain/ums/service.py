import bcrypt
from sqlalchemy.orm import Session

from app.core.exception import AuthWrongPasswordException, EmailNotVerifiedException, UserDuplicateException, UserNotFoundException
from app.domain.ums.model import User
from app.domain.ums.repository import EmailVerificationCodeRepository, UserRepository
from app.domain.ums.schema import ChangePasswordRequest, JoinRequest, JoinResponse, UpdateProfileRequest, UserMeResponse


class UserService:

    def __init__(self, db: Session):
        self.repository = UserRepository(db)
        self.email_verification_code_repository = EmailVerificationCodeRepository(db)

    def join(self, request: JoinRequest) -> JoinResponse:
        existing = self.repository.find_by_email(request.email)
        if existing:
            raise UserDuplicateException()

        verification = self.email_verification_code_repository.find_latest_by_email(request.email)
        if not verification or not verification.verified:
            raise EmailNotVerifiedException()

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

        self.email_verification_code_repository.delete_by_email(request.email)

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

    def update_profile(self, uid: int, request: UpdateProfileRequest) -> UserMeResponse:
        user = self.repository.find_by_id(uid)
        if not user:
            raise UserNotFoundException()

        user.nickname = request.nickname
        self.repository.db.flush()

        return UserMeResponse(
            uid=user.id,
            email=user.email,
            name=user.name,
            nickname=user.nickname,
            avatar=user.avatar,
            type=user.type.value,
            joined_date=user.joined_date,
        )

    def change_password(self, uid: int, request: ChangePasswordRequest) -> None:
        user = self.repository.find_by_id(uid)
        if not user:
            raise UserNotFoundException()

        if not bcrypt.checkpw(request.current_password.encode("utf-8"), user.password.encode("utf-8")):
            raise AuthWrongPasswordException()

        user.password = bcrypt.hashpw(
            request.new_password.encode("utf-8"), bcrypt.gensalt()
        ).decode("utf-8")
        self.repository.db.flush()
