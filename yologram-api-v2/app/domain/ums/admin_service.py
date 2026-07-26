import bcrypt
from sqlalchemy.orm import Session

from app.core.exception import (
    AdminUserDuplicateException,
    AdminUserNotFoundException,
    AuthWrongPasswordException,
)
from app.domain.ums.admin_jwt_util import create_admin_token
from app.domain.ums.admin_schema import (
    AdminAuthData,
    AdminLoginRequest,
    AdminLoginResponse,
    AdminUserCreateRequest,
    AdminUserCreateResponse,
    AdminValidateTokenResponse,
)
from app.domain.ums.model import AdminUser
from app.domain.ums.repository import AdminUserRepository


class AdminUserService:

    def __init__(self, db: Session):
        self.repository = AdminUserRepository(db)

    def create(self, request: AdminUserCreateRequest) -> AdminUserCreateResponse:
        existing = self.repository.find_by_email(request.email)
        if existing:
            raise AdminUserDuplicateException()

        hashed_password = bcrypt.hashpw(
            request.password.encode("utf-8"), bcrypt.gensalt()
        ).decode("utf-8")

        admin = AdminUser(
            email=request.email,
            name=request.name,
            password=hashed_password,
        )
        saved = self.repository.save(admin)

        return AdminUserCreateResponse(uid=saved.id)

    def login(self, request: AdminLoginRequest) -> AdminLoginResponse:
        admin = self.repository.find_by_email(request.email)
        if not admin:
            raise AdminUserNotFoundException()

        if not bcrypt.checkpw(request.password.encode("utf-8"), admin.password.encode("utf-8")):
            raise AuthWrongPasswordException()

        access_token = create_admin_token(admin.id)

        return AdminLoginResponse(
            uid=admin.id,
            access_token=access_token,
            email=admin.email,
            name=admin.name,
        )

    def validate_token(self, auth_data: AdminAuthData) -> AdminValidateTokenResponse:
        admin = self.repository.find_by_id(auth_data.uid)
        if not admin:
            raise AdminUserNotFoundException()

        return AdminValidateTokenResponse(
            uid=admin.id,
            email=admin.email,
            name=admin.name,
        )

    def logout(self, auth_data: AdminAuthData) -> None:
        pass
