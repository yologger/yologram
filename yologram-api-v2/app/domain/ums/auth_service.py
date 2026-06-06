import bcrypt
from sqlalchemy.orm import Session

from app.core.exception import AuthTokenInvalidException, AuthWrongPasswordException, UserNotFoundException
from app.domain.ums.auth_schema import AuthData, LoginRequest, LoginResponse, ValidateTokenResponse
from app.domain.ums.jwt_util import create_token
from app.domain.ums.repository import UserRepository


class AuthService:

    def __init__(self, db: Session):
        self.repository = UserRepository(db)

    def login(self, request: LoginRequest) -> LoginResponse:
        user = self.repository.find_by_email(request.email)
        if not user:
            raise UserNotFoundException()

        if not bcrypt.checkpw(request.password.encode("utf-8"), user.password.encode("utf-8")):
            raise AuthWrongPasswordException()

        access_token = create_token(user.id)
        user.access_token = access_token

        return LoginResponse(
            uid=user.id,
            access_token=access_token,
            email=user.email,
            name=user.name,
            nickname=user.nickname,
        )

    def validate_token(self, auth_data: AuthData) -> ValidateTokenResponse:
        user = self.repository.find_by_id(auth_data.uid)
        if not user:
            raise UserNotFoundException()

        if user.access_token != auth_data.access_token:
            raise AuthTokenInvalidException()

        return ValidateTokenResponse(
            uid=user.id,
            email=user.email,
            name=user.name,
            nickname=user.nickname,
        )

    def logout(self, auth_data: AuthData) -> None:
        user = self.repository.find_by_id(auth_data.uid)
        if not user:
            raise UserNotFoundException()
        user.access_token = None
